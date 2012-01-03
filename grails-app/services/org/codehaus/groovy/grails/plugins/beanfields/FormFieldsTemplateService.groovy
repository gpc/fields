package org.codehaus.groovy.grails.plugins.beanfields

import grails.util.GrailsNameUtils
import org.apache.commons.lang.ClassUtils
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.springframework.web.context.request.RequestContextHolder
import org.apache.commons.lang.builder.HashCodeBuilder
import org.apache.commons.lang.builder.EqualsBuilder

class FormFieldsTemplateService {

	static transactional = false

	GrailsConventionGroovyPageLocator groovyPageLocator
	GrailsPluginManager pluginManager

	Map findTemplate(BeanPropertyAccessor propertyAccessor, String templateName) {
		findTemplateCached(new BeanPropertyAccessorWrapper(propertyAccessor), controllerName, templateName)
	}

	private final Closure findTemplateCached = this.&findTemplateCacheable.memoize()

	private Map findTemplateCacheable(BeanPropertyAccessorWrapper propertyAccessor, String controllerName, String templateName) {
		def candidatePaths = candidateTemplatePaths(propertyAccessor, controllerName, templateName)

		def template = candidatePaths.findResult { path ->
			log.debug "looking for template with path $path"
			def source = groovyPageLocator.findTemplateByPath(path)
			source ? [source: source, path: path] : null
		}
		if (template) {
			def plugin = pluginManager.allPlugins.find {
				template.source.URI.startsWith(it.pluginPath)
			}
			template.plugin = plugin?.name
			log.info "found template $template.path ${plugin ? "in $template.plugin plugin" : ''}"
			template
		} else {
			log.warn "could not find a template for any of $candidatePaths"
			[:]
		}
	}

	static String toPropertyNameFormat(Class type) {
		GrailsNameUtils.getLogicalPropertyName(type.name, '')
	}

	private List<String> candidateTemplatePaths(BeanPropertyAccessor propertyAccessor, String controllerName, String templateName) {
		// order of priority for template resolution
		// 1: grails-app/views/controller/<property>/_field.gsp
		// 2: grails-app/views/forms/<class>.<property>/_field.gsp
		// 3: grails-app/views/forms/<anysuperclassclass>.<property>/_field.gsp
		// 4: grails-app/views/forms/<type>/_field.gsp, type is class' simpleName
		// 5: grails-app/views/forms/<anysupertype>/_field.gsp, type is class' simpleName
		// 6: grails-app/views/forms/default/_field.gsp
		def templateResolveOrder = []
		if (controllerName) {
			templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/", controllerName, propertyAccessor.propertyName, templateName)
		}
		templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/forms", toPropertyNameFormat(propertyAccessor.beanType), propertyAccessor.propertyName, templateName)
		for (superclass in propertyAccessor.beanSuperclasses) {
			templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/forms", toPropertyNameFormat(superclass), propertyAccessor.propertyName, templateName)
		}
		templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/forms", toPropertyNameFormat(propertyAccessor.propertyType), templateName)
		for (propertySuperClass in ClassUtils.getAllSuperclasses(propertyAccessor.propertyType)) {
			templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/forms", toPropertyNameFormat(propertySuperClass), templateName)
		}
		templateResolveOrder << "/forms/default/$templateName"
		templateResolveOrder
	}

	private String getControllerName() {
		RequestContextHolder.requestAttributes?.controllerName
	}

}

class BeanPropertyAccessorWrapper implements BeanPropertyAccessor {

	@Delegate private final BeanPropertyAccessor delegate

	BeanPropertyAccessorWrapper(BeanPropertyAccessor delegate) {
		this.delegate = delegate
	}

	@Override
	int hashCode() {
		def builder = new HashCodeBuilder()
		builder.append(delegate.beanType)
		builder.append(delegate.propertyName)
		builder.append(delegate.propertyType)
		builder.toHashCode()
	}

	@Override
	boolean equals(Object obj) {
		if (!(obj instanceof BeanPropertyAccessor)) return false
		def builder = new EqualsBuilder()
		builder.append(delegate.beanType, obj.beanType)
		builder.append(delegate.propertyName, obj.propertyName)
		builder.append(delegate.propertyType, obj.propertyType)
		builder.isEquals()
	}

}