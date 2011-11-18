package org.codehaus.groovy.grails.plugins.beanfields.taglib

import grails.artefact.Artefact
import javax.annotation.PostConstruct
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.springframework.beans.PropertyAccessorFactory
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.plugins.beanfields.*
import org.codehaus.groovy.grails.scaffolding.*

@Artefact("TagLibrary")
class FormFieldsTagLib implements GrailsApplicationAware {

	static namespace = "form"

	GrailsApplication grailsApplication
	GrailsConventionGroovyPageLocator groovyPageLocator
	BeanPropertyAccessorFactory beanPropertyAccessorFactory
	GrailsPluginManager pluginManager

	@PostConstruct
	void initialize() {
		beanPropertyAccessorFactory = new BeanPropertyAccessorFactory(grailsApplication: grailsApplication)
	}

	Closure bean = { attrs ->
		if (!attrs.bean) throwTagError("Tag [bean] is missing required attribute [bean]")
		def bean = resolveBean(attrs)
		def domainClass = resolveDomainClass(bean)

		for (property in resolvePersistentProperties(domainClass)) {
			if (property.embedded) {
				for (embeddedProp in resolvePersistentProperties(property.component)) {
					out << field(bean: bean, property: property.name)
				}
			} else {
				out << field(bean: bean, property: property.name)
			}
		}
	}

	Closure field = { attrs ->
		if (!attrs.bean) throwTagError("Tag [field] is missing required attribute [bean]")
		if (!attrs.property) throwTagError("Tag [field] is missing required attribute [property]")

		def propertyAccessor = resolveProperty(attrs)
		def model = buildModel(propertyAccessor, attrs)

		model.input = renderInput(propertyAccessor, model)

		def template = resolveFieldTemplate(propertyAccessor, "field")
		out << render(template: template, model: model)
	}

	Closure input = { attrs ->
		if (!attrs.bean) throwTagError("Tag [input] is missing required attribute [bean]")
		if (!attrs.property) throwTagError("Tag [input] is missing required attribute [property]")

		def propertyAccessor = resolveProperty(attrs)
		def model = buildModel(propertyAccessor, attrs)
		out << renderInput(propertyAccessor, model)
	}

	private BeanPropertyAccessor resolveProperty(attrs) {
		def bean = resolveBean(attrs)
		def propertyPath = attrs.property
		def propertyAccessor = beanPropertyAccessorFactory.accessorFor(bean, propertyPath)
		return propertyAccessor
	}

	private Map buildModel(BeanPropertyAccessor propertyAccessor, attrs) {
		[
				bean: propertyAccessor.rootBean,
				property: propertyAccessor.pathFromRoot,
				type: propertyAccessor.type,
				label: resolveLabelText(propertyAccessor, attrs),
				value: attrs.value ?: propertyAccessor.value ?: attrs.default,
				constraints: propertyAccessor.constraints,
				persistentProperty: propertyAccessor.persistentProperty,
				errors: propertyAccessor.errors.collect { message(error: it) },
				required: attrs.containsKey("required") ? Boolean.valueOf(attrs.required) : propertyAccessor.required,
				invalid: attrs.containsKey("invalid") ? Boolean.valueOf(attrs.invalid) : propertyAccessor.invalid,
		]
	}

	private String renderInput(BeanPropertyAccessor propertyAccessor, LinkedHashMap<String, Object> model) {
		def template = resolveFieldTemplate(propertyAccessor, "input")
		if (template) {
			return render(template: template, model: model)
		} else {
			return renderDefaultInput(model)
		}
	}

	// TODO: cache the result of this lookup
	private String resolveFieldTemplate(BeanPropertyAccessor propertyAccessor, String templateName) {
		// order of priority for template resolution
		// 1: grails-app/views/controller/<property>/_field.gsp
		// 2: grails-app/views/forms/<class>.<property>/_field.gsp
		// 3: grails-app/views/forms/<type>/_field.gsp
		// 4: grails-app/views/forms/default/_field.gsp
		// TODO: implications for templates supplied by plugins
		def templateResolveOrder = []
		templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/", controllerName, propertyAccessor.propertyName, templateName)
		templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/forms", propertyAccessor.beanClass.propertyName, propertyAccessor.propertyName, templateName)
		templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/forms", propertyAccessor.type.name, templateName)
		templateResolveOrder << "/forms/default/$templateName"

		def template = templateResolveOrder.find {
			groovyPageLocator.findTemplateByPath(it)
		}
		return template
	}

	private resolveBean(Map attrs) {
		pageScope.variables[attrs.bean] ?: attrs.bean
	}

	private GrailsDomainClass resolveDomainClass(bean) {
		resolveDomainClass(bean.getClass())
	}

	private GrailsDomainClass resolveDomainClass(Class beanClass) {
		grailsApplication.getArtefact("Domain", beanClass.simpleName)
	}

	private List<GrailsDomainClassProperty> resolvePersistentProperties(GrailsDomainClass domainClass) {
		boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate')
		def properties = domainClass.persistentProperties as List
		def comparator = hasHibernate ? new DomainClassPropertyComparator(domainClass) : new SimpleDomainClassPropertyComparator(domainClass)
		Collections.sort(properties, comparator)
		properties
	}

	private Map<String, Object> resolveProperty(bean, String property) {
		def domainClass = resolveDomainClass(bean.getClass())
		def path = property.tokenize(".")
		resolvePropertyFromPathComponents(PropertyAccessorFactory.forBeanPropertyAccess(bean), domainClass, path)
	}

	private String resolveLabelText(BeanPropertyAccessor propertyAccessor, Map attrs) {
		def label = attrs.label
		if (!label && attrs.labelKey) {
			label = message(code: attrs.labelKey)
		}
		label ?: message(code: propertyAccessor.labelKey, default: propertyAccessor.defaultLabel)
	}

	private String renderDefaultInput(Map attrs) {
		def model = [:]
		model.name = attrs.property
		model.value = attrs.value
		if (attrs.required) model.required = ""
		if (attrs.invalid) model.invalid = ""
		if (!attrs.constraints.editable) model.readonly = ""

		if (attrs.type in String) {
			return renderStringInput(model, attrs)
		} else if (attrs.type in [boolean, Boolean]) {
			return g.checkBox(model)
		} else if (attrs.type.isPrimitive() || attrs.type in Number) {
			return renderNumericInput(model, attrs)
		} else if (attrs.type in URL) {
			return g.field(model + [type: "url"])
		} else if (attrs.type.isEnum()) {
			model.from = attrs.type.values()
			if (!attrs.required) model.noSelection = ["": ""]
			return g.select(model)
		} else if (attrs.persistentProperty.oneToOne || attrs.persistentProperty.manyToOne || attrs.persistentProperty.manyToMany) {
			return renderAssociationInput(model, attrs)
		} else if (attrs.persistentProperty.oneToMany) {
			return renderOneToManyInput(model, attrs)
		} else if (attrs.type in [Date, Calendar, java.sql.Date, java.sql.Time]) {
			return renderDateTimeInput(model, attrs)
		} else if (attrs.type in [byte[], Byte[]]) {
			return g.field(model + [type: "file"])
		} else if (attrs.type in [TimeZone, Currency, Locale]) {
			if (!attrs.required) model.noSelection = ["": ""]
			return g."${StringUtils.uncapitalize(attrs.type.simpleName)}Select"(model)
		} else {
			return null
		}
	}

	private String renderDateTimeInput(Map model, Map attrs) {
		model.precision = attrs.type == java.sql.Time ? "minute" : "day"
		if (!attrs.required) {
			model.noSelection = ["": ""]
			model.default = "none"
		}
		return g.datePicker(model)
	}

	private String renderStringInput(Map model, Map attrs) {
		if (attrs.constraints.inList) {
			model.from = attrs.constraints.inList
			if (!attrs.required) model.noSelection = ["": ""]
			return g.select(model)
		} else if (attrs.constraints.password) model.type = "password"
		else if (attrs.constraints.email) model.type = "email"
		else if (attrs.constraints.url) model.type = "url"
		else model.type = "text"

		if (attrs.constraints.matches) model.pattern = attrs.constraints.matches
		if (attrs.constraints.maxSize) model.maxlength = attrs.constraints.maxSize

		return g.field(model)
	}

	private String renderNumericInput(Map model, Map attrs) {
		if (attrs.constraints.inList) {
			model.from = attrs.constraints.inList
			if (!attrs.required) model.noSelection = ["": ""]
			return g.select(model)
		} else if (attrs.constraints.range) {
			model.type = "range"
			model.min = attrs.constraints.range.from
			model.max = attrs.constraints.range.to
		} else {
			model.type = "number"
			if (attrs.constraints.min != null) model.min = attrs.constraints.min
			if (attrs.constraints.max != null) model.max = attrs.constraints.max
		}
		return g.field(model)
	}

	private String renderAssociationInput(Map model, Map attrs) {
		model.name = "${attrs.property}.id"
		model.id = attrs.property
		model.from = attrs.persistentProperty.referencedPropertyType.list()
		model.optionKey = "id" // TODO: handle alternate id names
		if (attrs.persistentProperty.manyToMany) {
			model.multiple = ""
			model.value = attrs.value*.id
		} else {
			if (!attrs.required) model.noSelection = ["null": ""]
			model.value = attrs.value?.id
		}
		return g.select(model)
	}

	private String renderOneToManyInput(Map model, Map attrs) {
		def buffer = new StringBuilder()
		buffer << '<ul>'
		def referencedDomainClass = attrs.persistentProperty.referencedDomainClass
		def controllerName = referencedDomainClass.propertyName
		model.value.each {
			buffer << '<li>'
			buffer << g.link(controller: controllerName, action: "show", id: it.id, it.toString().encodeAsHTML())
			buffer << '</li>'
		}
		buffer << '</ul>'
		def referencedTypeLabel = message(code: "${referencedDomainClass.propertyName}.label", default: referencedDomainClass.shortName)
		def addLabel = g.message(code: 'default.add.label', args: [referencedTypeLabel])
		buffer << g.link(controller: controllerName, action: "create", params: [("${attrs.beanDomainClass.propertyName}.id".toString()): attrs.bean.id], addLabel)
		buffer as String
	}

}

