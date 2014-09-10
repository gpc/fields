/*
 * Copyright 2012 Rob Fletcher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.plugin.formfields

import groovy.xml.MarkupBuilder
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.web.pages.FastStringWriter
import org.codehaus.groovy.grails.web.pages.GroovyPage

import static FormFieldsTemplateService.toPropertyNameFormat
import static org.codehaus.groovy.grails.commons.GrailsClassUtils.getStaticPropertyValue

import java.sql.Blob
class FormFieldsTagLib implements GrailsApplicationAware {

	static final namespace = 'f'
	static final String BEAN_PAGE_SCOPE_VARIABLE = 'f:with:bean'
	static final String PREFIX_PAGE_SCOPE_VARIABLE = 'f:with:prefix'

	FormFieldsTemplateService formFieldsTemplateService
	GrailsApplication grailsApplication
	BeanPropertyAccessorFactory beanPropertyAccessorFactory
	
	static defaultEncodeAs = [taglib:'raw']

	/**
	 * @attr bean REQUIRED Name of the source bean in the GSP model.
	 * @attr prefix Prefix to add to input element names.
	 */
	def with = { attrs, body ->
		if (!attrs.bean) throwTagError("Tag [with] is missing required attribute [bean]")
		def bean = resolveBean(attrs.bean)
		def prefix = resolvePrefix(attrs.prefix)
		try {
			pageScope.variables[BEAN_PAGE_SCOPE_VARIABLE] = bean
			pageScope.variables[PREFIX_PAGE_SCOPE_VARIABLE] = prefix
			out << body()
		} finally {
			pageScope.variables.remove(BEAN_PAGE_SCOPE_VARIABLE)
			pageScope.variables.remove(PREFIX_PAGE_SCOPE_VARIABLE)
		}
	}

	/**
	 * @attr bean REQUIRED Name of the source bean in the GSP model.
	 * @attr except A comma-separated list of properties to exclude from
	 * the generated list of input fields.
	 * @attr prefix Prefix to add to input element names.
	 */
	def all = { attrs ->
		if (!attrs.bean) throwTagError("Tag [all] is missing required attribute [bean]")
		def bean = resolveBean(attrs.bean)
		def prefix = resolvePrefix(attrs.prefix)
		def domainClass = resolveDomainClass(bean)
		if (domainClass) {
			for (property in resolvePersistentProperties(domainClass, attrs)) {
				out << field(bean: bean, property: property.name, prefix: prefix)
			}
		} else {
			throwTagError('Tag [all] currently only supports domain types')
		}
	}

	/**
	 * @attr bean Name of the source bean in the GSP model.
	 * @attr property REQUIRED The name of the property to display. This is resolved
	 * against the specified bean or the bean in the current scope.
	 * @attr value Specifies the initial value to display in the field. The default is
	 * the current value of the property.
	 * @attr default A default initial value to display if the actual property value
	 * evaluates to {@code false}.
	 * @attr required Specifies whether the user is required to enter a value for this
	 * property. By default, this is determined by the constraints of the property.
	 * @attr invalid Specifies whether this property is invalid or not. By default, this
	 * is determined by whether there are any errors associated with it.
	 * @attr label Overrides the default label displayed next to the input field.
	 * @attr prefix Prefix to add to input element names.
	 */
	def field = { attrs, body ->
		if (attrs.containsKey('bean') && !attrs.bean) throwTagError("Tag [field] requires a non-null value for attribute [bean]")
		if (!attrs.property) throwTagError("Tag [field] is missing required attribute [property]")

		def bean = resolveBean(attrs.remove('bean'))
		def property = attrs.remove('property')

		def propertyAccessor = resolveProperty(bean, property)
		if (propertyAccessor.persistentProperty?.embedded) {
			renderEmbeddedProperties(bean, propertyAccessor, attrs)
		} else {
			def model = buildModel(propertyAccessor, attrs)
			def fieldAttrs = [:]
			def inputAttrs = [:]

			attrs.each { k, v ->
				if (k?.startsWith("input-"))
					inputAttrs[k.replace("input-", '')] = v
				else
					fieldAttrs[k] = v
			}

			if (hasBody(body)) {
                model.widget = body(model + [attrs: inputAttrs] + inputAttrs)
            } else {
				model.widget = renderWidget(propertyAccessor, model, inputAttrs)
			}

			def template = formFieldsTemplateService.findTemplate(propertyAccessor, 'field')
			if (template) {
				out << render(template: template.path, plugin: template.plugin, model: model + [attrs: fieldAttrs] + fieldAttrs)
			} else {
				out << renderDefaultField(model)
			}
		}
	}

	/**
	 * @attr bean REQUIRED Name of the source bean in the GSP model.
	 * @attr property REQUIRED The name of the property to display. This is resolved
	 * against the specified bean or the bean in the current scope.
	 */
	def input = { attrs ->
		def bean = resolveBean(attrs.remove('bean'))
		if (!bean) throwTagError("Tag [input] is missing required attribute [bean]")
		if (!attrs.property) throwTagError("Tag [input] is missing required attribute [property]")

		def property = attrs.remove('property')

		def propertyAccessor = resolveProperty(bean, property)
		def model = buildModel(propertyAccessor, attrs)

		out << renderWidget(propertyAccessor, model, attrs)
	}

	def display = { attrs, body ->
		def bean = resolveBean(attrs.remove('bean'))
		if (!bean) throwTagError("Tag [display] is missing required attribute [bean]")
		if (!attrs.property) throwTagError("Tag [display] is missing required attribute [property]")

		def property = attrs.remove('property')

		def propertyAccessor = resolveProperty(bean, property)
		def model = buildModel(propertyAccessor, attrs)

		if (hasBody(body)) {
			model.value = body(model)
		} else {
			model.value = renderDefaultDisplay(model, attrs)
		}

		out << renderForDisplay(propertyAccessor, model, attrs)
	}

	private void renderEmbeddedProperties(bean, BeanPropertyAccessor propertyAccessor, attrs) {
		def legend = resolveMessage(propertyAccessor.labelKeys, propertyAccessor.defaultLabel)
		out << applyLayout(name: '_fields/embedded', params: [type: toPropertyNameFormat(propertyAccessor.propertyType), legend: legend]) {
			for (embeddedProp in resolvePersistentProperties(propertyAccessor.persistentProperty.component, attrs)) {
				def propertyPath = "${propertyAccessor.pathFromRoot}.${embeddedProp.name}"
				out << field(bean: bean, property: propertyPath, prefix: attrs.prefix)
			}
		}
	}

	private BeanPropertyAccessor resolveProperty(bean, String propertyPath) {
		beanPropertyAccessorFactory.accessorFor(bean, propertyPath)
	}

	private Map buildModel(BeanPropertyAccessor propertyAccessor, Map attrs) {
		def value = attrs.containsKey('value') ? attrs.remove('value') : propertyAccessor.value
		def valueDefault = attrs.remove('default')
		[
			bean: propertyAccessor.rootBean,
			property: propertyAccessor.pathFromRoot,
			type: propertyAccessor.propertyType,
			beanClass: propertyAccessor.beanClass,
			label: resolveLabelText(propertyAccessor, attrs),
			value: (value instanceof Number || value instanceof Boolean || value) ? value : valueDefault,
			constraints: propertyAccessor.constraints,
			persistentProperty: propertyAccessor.persistentProperty,
			errors: propertyAccessor.errors.collect { message(error: it) },
			required: attrs.containsKey("required") ? Boolean.valueOf(attrs.remove('required')) : propertyAccessor.required,
			invalid: attrs.containsKey("invalid") ? Boolean.valueOf(attrs.remove('invalid')) : propertyAccessor.invalid,
			prefix: resolvePrefix(attrs.remove('prefix')),
		]
	}

    private CharSequence renderWidget(BeanPropertyAccessor propertyAccessor, Map model, Map attrs = [:]) {
		def template = formFieldsTemplateService.findTemplate(propertyAccessor, 'input')
		if (template) {
			render template: template.path, plugin: template.plugin, model: model + [attrs: attrs] + attrs
		} else {
			renderDefaultInput model, attrs
		}
	}

	private CharSequence renderForDisplay(BeanPropertyAccessor propertyAccessor, Map model, Map attrs = [:]) {
		def template = formFieldsTemplateService.findTemplate(propertyAccessor, 'display')
		if (template) {
			render template: template.path, plugin: template.plugin, model: model + [attrs: attrs] + attrs
		} else {
			model.value
		}
	}

	private Object resolveBean(beanAttribute) {
		def bean = pageScope.variables[BEAN_PAGE_SCOPE_VARIABLE]
		if (!bean) {
			// Tomcat throws NPE if you query pageScope for null/empty values
			if (beanAttribute.toString()) {
				bean = pageScope.variables[beanAttribute]
			}
		}
		if (!bean) bean = beanAttribute
		bean
	}

	private String resolvePrefix(prefixAttribute) {
		def prefix = pageScope.variables[PREFIX_PAGE_SCOPE_VARIABLE]
		// Tomcat throws NPE if you query pageScope for null/empty values
		if (prefixAttribute?.toString()) {
			prefix = pageScope.variables[prefixAttribute]
		}
		if (!prefix) prefix = prefixAttribute
		if (prefix && !prefix.endsWith('.'))
			prefix = prefix + '.'
		prefix ?: ''
	}

	private GrailsDomainClass resolveDomainClass(bean) {
		resolveDomainClass(bean.getClass())
	}

	private GrailsDomainClass resolveDomainClass(Class beanClass) {
		grailsApplication.getDomainClass(beanClass.name)
	}

	private List<GrailsDomainClassProperty> resolvePersistentProperties(GrailsDomainClass domainClass, attrs) {
		def properties = domainClass.persistentProperties as List

		def blacklist = attrs.except?.tokenize(',')*.trim() ?: []
		blacklist << 'dateCreated' << 'lastUpdated'
		def scaffoldProp = getStaticPropertyValue(domainClass.clazz, 'scaffold')
		if (scaffoldProp) {
			blacklist.addAll(scaffoldProp.exclude)
		}
		properties.removeAll { it.name in blacklist }
		properties.removeAll { !it.domainClass.constrainedProperties[it.name]?.display }
        properties.removeAll { it.derived }

		Collections.sort(properties, new DomainClassPropertyComparator(domainClass))
		properties
	}

	private boolean hasBody(Closure body) {
		return body && !body.is(GroovyPage.EMPTY_BODY_CLOSURE)
	}

	private CharSequence resolveLabelText(BeanPropertyAccessor propertyAccessor, Map attrs) {
		def labelText
		def label = attrs.remove('label')
		if (label) {
			labelText = message(code: label, default: label)
		}
		if (!labelText && propertyAccessor.labelKeys) {
			labelText = resolveMessage(propertyAccessor.labelKeys, propertyAccessor.defaultLabel)
		}
		if (!labelText) {
			labelText = propertyAccessor.defaultLabel
		}
		labelText
	}

	private CharSequence resolveMessage(List<String> keysInPreferenceOrder, String defaultMessage) {
		def message = keysInPreferenceOrder.findResult { key ->
			message(code: key, default: null) ?: null
		}
		message ?: defaultMessage
	}

	private CharSequence renderDefaultField(Map model) {
		def classes = ['fieldcontain']
		if (model.invalid) classes << 'error'
		if (model.required) classes << 'required'

		def writer = new FastStringWriter()
		new MarkupBuilder(writer).div(class: classes.join(' ')) {
			label(for: (model.prefix ?: '') + model.property, model.label) {
				if (model.required) {
					span(class: 'required-indicator', '*')
				}
			}
			// TODO: encoding information of widget gets lost - don't use MarkupBuilder
			mkp.yieldUnescaped model.widget 
		}
		writer.buffer
	}

	private CharSequence renderDefaultInput(Map model, Map attrs = [:]) {
		attrs.name = (model.prefix ?: '') + model.property
		attrs.value = model.value
		if (model.required) attrs.required = "" // TODO: configurable how this gets output? Some people prefer required="required"
		if (model.invalid) attrs.invalid = ""
		if (!isEditable(model.constraints)) attrs.readonly = ""

		if (model.type in [String, null]) {
			return renderStringInput(model, attrs)
		} else if (model.type in [boolean, Boolean]) {
			return g.checkBox(attrs)
		} else if (model.type.isPrimitive() || model.type in Number) {
			return renderNumericInput(model, attrs)
		} else if (model.type in URL) {
			return g.field(attrs + [type: "url"])
		} else if (model.type.isEnum()) {
			return renderEnumInput(model,attrs)
		} else if (model.persistentProperty?.oneToOne || model.persistentProperty?.manyToOne || model.persistentProperty?.manyToMany) {
			return renderAssociationInput(model, attrs)
		} else if (model.persistentProperty?.oneToMany) {
			return renderOneToManyInput(model, attrs)
		} else if (model.type in [Date, Calendar, java.sql.Date, java.sql.Time]) {
			return renderDateTimeInput(model, attrs)
		} else if (model.type in [byte[], Byte[], Blob]) {
			return g.field(attrs + [type: "file"])
		} else if (model.type in [TimeZone, Currency, Locale]) {
			if (!model.required) attrs.noSelection = ["": ""]
			return g."${StringUtils.uncapitalize(model.type.simpleName)}Select"(attrs)
		} else {
			return null
		}
	}

    private CharSequence renderDateTimeInput(Map model, Map attrs) {
		attrs.precision = model.type == java.sql.Time ? "minute" : "day"
		if (!model.required) {
			attrs.noSelection = ["": ""]
			attrs.default = "none"
		}
		return g.datePicker(attrs)
	}

	private CharSequence renderStringInput(Map model, Map attrs) {
		if (!attrs.type) {
			if (model.constraints?.inList) {
				attrs.from = model.constraints.inList
				if (!model.required) attrs.noSelection = ["": ""]
				return g.select(attrs)
			}
			else if (model.constraints?.password) {
				attrs.type = "password"
				attrs.remove('value')
			}
			else if (model.constraints?.email) attrs.type = "email"
			else if (model.constraints?.url) attrs.type = "url"
			else attrs.type = "text"
		}

		if (model.constraints?.matches) attrs.pattern = model.constraints.matches
		if (model.constraints?.maxSize) attrs.maxlength = model.constraints.maxSize

		if (model.constraints?.widget == 'textarea') {
			attrs.remove('type')
			return g.textArea(attrs)
		}
		return g.field(attrs)
	}

	private CharSequence renderNumericInput(Map model, Map attrs) {
		if (!attrs.type && model.constraints?.inList) {
			attrs.from = model.constraints.inList
			if (!model.required) attrs.noSelection = ["": ""]
			return g.select(attrs)
		} else if (model.constraints?.range) {
			attrs.type = attrs.type ?: "range"
			attrs.min = model.constraints.range.from
			attrs.max = model.constraints.range.to
		} else {
			attrs.type = attrs.type ?: "number"
			if (model.constraints?.scale != null) attrs.step = "0.${'0' * (model.constraints.scale - 1)}1"
			if (model.constraints?.min != null) attrs.min = model.constraints.min
			if (model.constraints?.max != null) attrs.max = model.constraints.max
		}
		return g.field(attrs)
	}

	private CharSequence renderEnumInput(Map model, Map attrs) {
		if (attrs.value instanceof Enum)
			attrs.value = attrs.value.name()
		if (!model.required) attrs.noSelection = ["": ""]

		if ( model.constraints?.inList) {
			attrs.keys = model.constraints.inList*.name()
			attrs.from = model.constraints.inList
		} else {
			attrs.keys = model.type.values()*.name()
			attrs.from = model.type.values()
		}
		return g.select(attrs)
	}

	private CharSequence renderAssociationInput(Map model, Map attrs) {
		attrs.id = (model.prefix ?: '') + model.property
		attrs.from = null != attrs.from ? attrs.from : model.persistentProperty.referencedPropertyType.list()
		attrs.optionKey = "id" // TODO: handle alternate id names
		if (model.persistentProperty.manyToMany) {
			attrs.multiple = ""
			attrs.value = model.value*.id
			attrs.name = "${model.prefix ?: ''}${model.property}"
		} else {
			if (!model.required) attrs.noSelection = ["null": ""]
			attrs.value = model.value?.id
			attrs.name = "${model.prefix ?: ''}${model.property}.id"
		}
		return g.select(attrs)
	}

	private CharSequence renderOneToManyInput(Map model, Map attrs) {
		def buffer = new FastStringWriter()
		buffer << '<ul>'
		def referencedDomainClass = model.persistentProperty.referencedDomainClass
		def controllerName = referencedDomainClass.propertyName
		attrs.value.each {
			buffer << '<li>'
			buffer << g.link(controller: controllerName, action: "show", id: it.id, it.toString().encodeAsHTML())
			buffer << '</li>'
		}
		buffer << '</ul>'
		def referencedTypeLabel = message(code: "${referencedDomainClass.propertyName}.label", default: referencedDomainClass.shortName)
		def addLabel = g.message(code: 'default.add.label', args: [referencedTypeLabel])
		buffer << g.link(controller: controllerName, action: "create", params: [("${model.beanClass.propertyName}.id".toString()): model.bean.id], addLabel)
		buffer.buffer
	}

	private CharSequence renderDefaultDisplay(Map model, Map attrs = [:]) {
		switch (model.type) {
			case Boolean.TYPE:
			case Boolean:
				g.formatBoolean(boolean: model.value)
				break
			case Calendar:
			case Date:
			case java.sql.Date:
			case java.sql.Time:
				g.formatDate(date: model.value)
				break
			default:
				g.fieldValue bean: model.bean, field: model.property
		}
	}

    private boolean isEditable(constraints) {
        !constraints || constraints.editable
    }

}
