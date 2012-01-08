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

import grails.util.GrailsNameUtils
import grails.util.Environment
import org.apache.commons.lang.ClassUtils
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.springframework.web.context.request.RequestContextHolder

class FormFieldsTemplateService {

	static transactional = false

	GrailsConventionGroovyPageLocator groovyPageLocator
	GrailsPluginManager pluginManager

	Map findTemplate(BeanPropertyAccessor propertyAccessor, String templateName) {
		findTemplateCached(propertyAccessor, controllerName, templateName)
	}

	private final Closure findTemplateCached = Environment.current == Environment.DEVELOPMENT ? this.&findTemplateCacheable : this.&findTemplateCacheable.memoize()

	private Map findTemplateCacheable(BeanPropertyAccessor propertyAccessor, String controllerName, String templateName) {
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
		// 2: grails-app/views/fields/<class>/<property>/_field.gsp
		// 3: grails-app/views/fields/<anysuperclassclass>.<property>/_field.gsp
		// 4: grails-app/views/fields/<type>/_field.gsp, type is class' simpleName
		// 5: grails-app/views/fields/<anysupertype>/_field.gsp, type is class' simpleName
		// 6: grails-app/views/fields/default/_field.gsp
		def templateResolveOrder = []
		if (controllerName) {
			templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/", controllerName, propertyAccessor.propertyName, templateName)
		}
		templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/fields", toPropertyNameFormat(propertyAccessor.beanType), propertyAccessor.propertyName, templateName)
		for (superclass in propertyAccessor.beanSuperclasses) {
			templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/fields", toPropertyNameFormat(superclass), propertyAccessor.propertyName, templateName)
		}
		templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/fields", toPropertyNameFormat(propertyAccessor.propertyType), templateName)
		for (propertySuperClass in ClassUtils.getAllSuperclasses(propertyAccessor.propertyType)) {
			templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/fields", toPropertyNameFormat(propertySuperClass), templateName)
		}
		templateResolveOrder << "/fields/default/$templateName"
		templateResolveOrder
	}

	private String getControllerName() {
		RequestContextHolder.requestAttributes?.controllerName
	}

}
