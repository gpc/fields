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
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator
import org.codehaus.groovy.grails.web.pages.GroovyPage
import static FormFieldsTemplateService.toPropertyNameFormat
import org.codehaus.groovy.grails.commons.*
import static org.codehaus.groovy.grails.commons.GrailsClassUtils.getStaticPropertyValue

import org.grails.datastore.mapping.model.types.OneToMany
import org.grails.datastore.mapping.model.types.ManyToMany
import org.grails.datastore.mapping.model.types.ManyToOne
import org.grails.datastore.mapping.model.types.OneToOne
import org.grails.datastore.mapping.model.PersistentProperty

class FormFieldsTagLib implements GrailsApplicationAware {

	static final namespace = 'f'
	static final String BEAN_PAGE_SCOPE_VARIABLE = 'f:with:bean'
	static final String PREFIX_PAGE_SCOPE_VARIABLE = 'f:with:prefix'

	FormFieldsTemplateService formFieldsTemplateService
	GrailsApplication grailsApplication
	BeanPropertyAccessorFactory beanPropertyAccessorFactory

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

	def all = { attrs ->
		if (!attrs.bean) throwTagError("Tag [all] is missing required attribute [bean]")
		def bean = resolveBean(attrs.bean)
		def prefix = resolvePrefix(attrs.prefix)
		def domainClass = resolveDomainClass(bean)
        def fieldTemplateName = attrs.remove('fieldTemplateName')
        def widgetTemplateName = attrs.remove('widgetTemplateName')
		if (domainClass) {
			for (property in resolvePersistentProperties(domainClass, attrs)) {
				out << field(bean: bean, property: property.name, prefix:prefix,
                        fieldTemplateName: fieldTemplateName, widgetTemplateName: widgetTemplateName)
			}
		} else {
			throwTagError('Tag [all] currently only supports domain types')
		}
	}

	def field = { attrs, body ->
		if (attrs.containsKey('bean') && !attrs.bean) throwTagError("Tag [field] requires a non-null value for attribute [bean]")
		if (!attrs.property) throwTagError("Tag [field] is missing required attribute [property]")

		def bean = attrs.remove('bean')
		def property = attrs.remove('property')
		def fieldTemplateName = attrs.remove('fieldTemplateName') ?: 'field'
        def widgetTemplateName = attrs.remove('widgetTemplateName') ?: 'input'

        def propertyAccessor = resolveProperty(bean, property)
        if (propertyAccessor.persistentProperty?.embedded) {
            renderEmbeddedProperties(bean, propertyAccessor, attrs, fieldTemplateName, widgetTemplateName)
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
				model.widget = body(model+inputAttrs)
			} else {
				model.widget = renderWidget(propertyAccessor, model, inputAttrs, widgetTemplateName)
			}

			def template = formFieldsTemplateService.findTemplate(propertyAccessor, fieldTemplateName)
			if (template) {
				out << render(template: template.path, plugin: template.plugin, model: model+fieldAttrs)
			} else {
				out << renderDefaultField(model, fieldTemplateName)
			}
		}
	}

	private void renderEmbeddedProperties(bean, BeanPropertyAccessor propertyAccessor, attrs, fieldTemplateName, widgetTemplateName) {
		def legend = resolveMessage(propertyAccessor.labelKeys, propertyAccessor.defaultLabel)
		out << applyLayout(name: '_fields/embedded', params: [type: toPropertyNameFormat(propertyAccessor.propertyType), legend: legend]) {
			for (embeddedProp in resolvePersistentProperties(propertyAccessor.persistentProperty.component, attrs)) {
				def propertyPath = "${propertyAccessor.pathFromRoot}.${embeddedProp.name}"
				out << field(bean: bean, property: propertyPath, prefix:attrs.prefix,
                        fieldTemplateName: fieldTemplateName, widgetTemplateName: widgetTemplateName)
			}
		}
	}

	def input = { attrs ->
		if (!attrs.bean) throwTagError("Tag [$name] is missing required attribute [bean]")
		if (!attrs.property) throwTagError("Tag [$name] is missing required attribute [property]")

		def bean = attrs.remove('bean')
		def property = attrs.remove('property')

		def propertyAccessor = resolveProperty(bean, property)
		def model = buildModel(propertyAccessor, attrs)

		out << renderWidget(propertyAccessor, model, attrs)
	}

	private BeanPropertyAccessor resolveProperty(beanAttribute, String propertyPath) {
		def bean = resolveBean(beanAttribute)
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
				value: (value instanceof Number || value) ? value : valueDefault,
				constraints: propertyAccessor.constraints,
				persistentProperty: propertyAccessor.persistentProperty,
				errors: propertyAccessor.errors.collect { message(error: it) },
				required: attrs.containsKey("required") ? Boolean.valueOf(attrs.remove('required')) : propertyAccessor.required,
				invalid: attrs.containsKey("invalid") ? Boolean.valueOf(attrs.remove('invalid')) : propertyAccessor.invalid,
				prefix: resolvePrefix(attrs.remove('prefix')),
		]
	}

	private String renderWidget(BeanPropertyAccessor propertyAccessor, Map model, Map attrs = [:], String templateName='input') {
        def template = formFieldsTemplateService.findTemplate(propertyAccessor, templateName)
		if (template) {
			render template: template.path, plugin: template.plugin, model: model + attrs
		} else {

            switch (templateName) {
                case 'show':
                    return renderDefaultShow(model, attrs)
                default:
                    return renderDefaultInput(model, attrs)
            }
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
			prefix = prefix+'.'
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
		properties.removeAll { !it.domainClass.constrainedProperties[it.name].display }

		Collections.sort(properties, new DomainClassPropertyComparator(domainClass))
		properties
	}

	private boolean hasBody(Closure body) {
		return !body.is(GroovyPage.EMPTY_BODY_CLOSURE)
	}

	private String resolveLabelText(BeanPropertyAccessor propertyAccessor, Map attrs) {
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
	
	private String resolveMessage(List<String> keysInPreferenceOrder, String defaultMessage) {
		def message = keysInPreferenceOrder.findResult { key ->
			message code: key, default: null
		}
		message ?: defaultMessage
	}

	private String renderDefaultField(Map model, String templateName='field') {
		def classes = ['fieldcontain']
		if (model.invalid) classes << 'error'
		if (model.required) classes << 'required'

		def writer = new StringWriter()
        def builder = new MarkupBuilder(writer)

        switch (templateName) {
            case 'showField':
                builder.li(class: classes.join(' ')) {
                    span(id: "${model.property}-label", class: 'property-label', model.label)
                    span(class:'property-value', 'aria-labelledby': "${model.property}-label") {
                        //"HURZ"
                        mkp.yieldUnescaped model.widget
                    }
                }
                break
            default:
                builder.div(class: classes.join(' ')) {
                    label(for: (model.prefix?:'')+model.property, model.label) {
                        if (model.required) {
                            span(class: 'required-indicator', '*')
                        }
                    }
                    mkp.yieldUnescaped model.widget
                }
        }
		writer.toString()
	}

	private String renderDefaultInput(Map model, Map attrs = [:]) {
		attrs.name = (model.prefix?:'')+model.property
		attrs.value = model.value
		if (model.required) attrs.required = "" // TODO: configurable how this gets output? Some people prefer required="required"
		if (model.invalid) attrs.invalid = ""
		if (!model.constraints.editable) attrs.readonly = ""

		if (model.type in [String, null]) {
			return renderStringInput(model, attrs)
		} else if (model.type in [boolean, Boolean]) {
			return g.checkBox(attrs)
		} else if (model.type.isPrimitive() || model.type in Number) {
			return renderNumericInput(model, attrs)
		} else if (model.type in URL) {
			return g.field(attrs + [type: "url"])
		} else if (model.type.isEnum()) {
			if (attrs.value instanceof Enum)
				attrs.value = attrs.value.name()
			attrs.keys = model.type.values()*.name()
			attrs.from = model.type.values()
			if (!model.required) attrs.noSelection = ["": ""]
			return g.select(attrs)
		} else if (model.persistentProperty?.oneToOne || model.persistentProperty?.manyToOne || model.persistentProperty?.manyToMany) {
			return renderAssociationInput(model, attrs)
		} else if (model.persistentProperty?.oneToMany) {
			return renderOneToManyInput(model, attrs)
		} else if (model.type in [Date, Calendar, java.sql.Date, java.sql.Time]) {
			return renderDateTimeInput(model, attrs)
		} else if (model.type in [byte[], Byte[]]) {
			return g.field(attrs + [type: "file"])
		} else if (model.type in [TimeZone, Currency, Locale]) {
			if (!model.required) attrs.noSelection = ["": ""]
			return g."${StringUtils.uncapitalize(model.type.simpleName)}Select"(attrs)
		} else {
			return null
		}
	}

    private String renderDefaultShow(Map model, Map attrs = [:]) {
        def name = (model.prefix ?: '') + model.property
        def value = model.value
        GrailsDomainClassProperty persistentProperty = model.persistentProperty
        def bean = model.bean

        if (persistentProperty?.oneToMany || persistentProperty?.manyToMany) {
            def buffer = new StringBuilder()
            def controllerName = persistentProperty.referencedDomainClass.propertyName
            buffer << '<ul>'
            value.each {
                buffer << '<li>'
                buffer << g.link(controller: controllerName, action: "show", id: it.id, it.toString().encodeAsHTML())
                buffer << '</li>'
            }
            buffer << '</ul>'
            return buffer.toString()
        } else if (persistentProperty?.manyToOne || persistentProperty?.oneToOne) {
            def controllerName = persistentProperty.referencedDomainClass.propertyName
            return g.link(controller: controllerName, action: 'show', id: value.id, value.toString().encodeAsHTML());
        } else {
            return g.fieldValue(bean: bean, field: name)
        }

        /*
        snippet from Grails' orig src/templates/scaffolding/show.gsp
                           <span id="${p.name}-label" class="property-label"><g:message
                        code="${domainClass.propertyName}.${p.name}.label" default="${p.naturalName}"/></span>
                <% if (p.isEnum()) { %>
                <span class="property-value" aria-labelledby="${p.name}-label"><g:fieldValue bean="\${${propertyName}}"
                                                                                             field="${p.name}"/></span>
                <% } else if (p.oneToMany || p.manyToMany) { %>
                <g:each in="\${${propertyName}.${p.name}}" var="${p.name[0]}">
                    <span class="property-value" aria-labelledby="${p.name}-label"><g:link
                            controller="${p.referencedDomainClass?.propertyName}" action="show"
                            id="\${${p.name[0]}.id}">\${${p.name[0]}?.encodeAsHTML()}</g:link></span>
                </g:each>
                <% } else if (p.manyToOne || p.oneToOne) { %>
                <span class="property-value" aria-labelledby="${p.name}-label"><g:link
                        controller="${p.referencedDomainClass?.propertyName}" action="show"
                        id="\${${propertyName}?.${p.name}?.id}">\${${propertyName}?.${p.name}?.encodeAsHTML()}</g:link></span>
                <% } else if (p.type == Boolean || p.type == boolean) { %>
                <span class="property-value" aria-labelledby="${p.name}-label"><g:formatBoolean
                        boolean="\${${propertyName}?.${p.name}}"/></span>
                <% } else if (p.type == Date || p.type == java.sql.Date || p.type == java.sql.Time || p.type == Calendar) { %>
                <span class="property-value" aria-labelledby="${p.name}-label"><g:formatDate
                        date="\${${propertyName}?.${p.name}}"/></span>
                <% } else if (!p.type.isArray()) { %>
                <span class="property-value" aria-labelledby="${p.name}-label"><g:fieldValue bean="\${${propertyName}}"
                                                                                             field="${p.name}"/></span>
                <% } %>
        */

    }

    private String renderDateTimeInput(Map model, Map attrs) {
		attrs.precision = model.type == java.sql.Time ? "minute" : "day"
		if (!model.required) {
			attrs.noSelection = ["": ""]
			attrs.default = "none"
		}
		return g.datePicker(attrs)
	}

	private String renderStringInput(Map model, Map attrs) {
		if (!attrs.type) {
			if (model.constraints.inList) {
				attrs.from = model.constraints.inList
				if (!model.required) attrs.noSelection = ["": ""]
				return g.select(attrs)
			}
			else if (model.constraints.password) {
				attrs.type = "password"
				attrs.remove('value')
			}
			else if (model.constraints.email) attrs.type = "email"
			else if (model.constraints.url) attrs.type = "url"
			else attrs.type = "text"
		}

		if (model.constraints.matches) attrs.pattern = model.constraints.matches
		if (model.constraints.maxSize) attrs.maxlength = model.constraints.maxSize
		
		if (model.constraints.widget == 'textarea') {
			attrs.remove('type')
			return g.textArea(attrs)
		}
		return g.field(attrs)
	}

	private String renderNumericInput(Map model, Map attrs) {
		if (!attrs.type && model.constraints.inList) {
			attrs.from = model.constraints.inList
			if (!model.required) attrs.noSelection = ["": ""]
			return g.select(attrs)
		} else if (model.constraints.range) {
			attrs.type = attrs.type ?: "range"
			attrs.min = model.constraints.range.from
			attrs.max = model.constraints.range.to
		} else {
			attrs.type = attrs.type ?: "number"
			if (model.constraints.min != null) attrs.min = model.constraints.min
			if (model.constraints.max != null) attrs.max = model.constraints.max
		}
		return g.field(attrs)
	}

	private String renderAssociationInput(Map model, Map attrs) {
		attrs.name = "${model.prefix?:''}${model.property}.id"
		attrs.id = (model.prefix?:'')+model.property
		attrs.from = attrs.from ?: model.persistentProperty.referencedPropertyType.list()
		attrs.optionKey = "id" // TODO: handle alternate id names
		if (model.persistentProperty.manyToMany) {
			attrs.multiple = ""
			attrs.value = model.value*.id
		} else {
			if (!model.required) attrs.noSelection = ["null": ""]
			attrs.value = model.value?.id
		}
		return g.select(attrs)
	}

	private String renderOneToManyInput(Map model, Map attrs) {
		def buffer = new StringBuilder()
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
		buffer as String
	}

}