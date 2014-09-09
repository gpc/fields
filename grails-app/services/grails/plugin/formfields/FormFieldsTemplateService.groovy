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
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.springframework.web.context.request.RequestContextHolder

import static org.codehaus.groovy.grails.io.support.GrailsResourceUtils.appendPiecesForUri

class FormFieldsTemplateService {

    static transactional = false

    def grailsApplication

    GrailsConventionGroovyPageLocator groovyPageLocator
    GrailsPluginManager pluginManager

    Map findTemplate(BeanPropertyAccessor propertyAccessor, String templateName) {
        findTemplateCached(propertyAccessor, controllerName, actionName, templateName)
    }

    private
    final Closure findTemplateCached = shouldCache() ? this.&findTemplateCacheable.memoize() : this.&findTemplateCacheable

    private Map findTemplateCacheable(BeanPropertyAccessor propertyAccessor, String controllerName, String actionName, String templateName) {
        def candidatePaths = candidateTemplatePaths(propertyAccessor, controllerName, actionName, templateName)

        candidatePaths.findResult { path ->
            log.debug "looking for template with path $path"
            def source = groovyPageLocator.findTemplateByPath(path)
            if (source) {
                def template = [path: path]
                def plugin = pluginManager.allPlugins.find {
                    source.URI.startsWith(it.pluginPath)
                }
                template.plugin = plugin?.name
                log.info "found template $template.path ${plugin ? "in $template.plugin plugin" : ''}"
                template
            } else {
                null
            }
        }
    }

    static String toPropertyNameFormat(Class type) {
        def propertyNameFormat = GrailsNameUtils.getLogicalPropertyName(type.name, '')
        if(propertyNameFormat.endsWith(';')) {
            propertyNameFormat = propertyNameFormat - ';' + 'Array'
        }
        return propertyNameFormat
    }

    private List<String> candidateTemplatePaths(BeanPropertyAccessor propertyAccessor, String controllerName, String actionName, String templateName) {
        def templateResolveOrder = []

        // if there is a controller for the current request any template in its views directory takes priority
        if (controllerName) {
            // first try action-specific templates
            templateResolveOrder << appendPiecesForUri("/", controllerName, actionName, propertyAccessor.propertyName, templateName)
            if (propertyAccessor.propertyType) templateResolveOrder << appendPiecesForUri("/", controllerName, actionName, toPropertyNameFormat(propertyAccessor.propertyType), templateName)
            templateResolveOrder << appendPiecesForUri("/", controllerName, actionName, templateName)

            // then general templates for the controller
            templateResolveOrder << appendPiecesForUri("/", controllerName, propertyAccessor.propertyName, templateName)
            if (propertyAccessor.propertyType) templateResolveOrder << appendPiecesForUri("/", controllerName, toPropertyNameFormat(propertyAccessor.propertyType), templateName)
            templateResolveOrder << appendPiecesForUri("/", controllerName, templateName)
        }

        // if we have a bean type look in `grails-app/views/_fields/<beanType>/<propertyName>/_field.gsp` and equivalent for superclasses
        if (propertyAccessor.beanType) {
            templateResolveOrder << appendPiecesForUri("/_fields", toPropertyNameFormat(propertyAccessor.beanType), propertyAccessor.propertyName, templateName)
            for (superclass in propertyAccessor.beanSuperclasses) {
                templateResolveOrder << appendPiecesForUri("/_fields", toPropertyNameFormat(superclass), propertyAccessor.propertyName, templateName)
            }
        }

        // if this is an association property look in `grails-app/views/_fields/<associationType>/_field.gsp`
        def associationPath = getAssociationPath(propertyAccessor)
        if (associationPath) {
            templateResolveOrder << appendPiecesForUri('/_fields', associationPath, templateName)
        }

        // if we have a widget look in `grails-app/views/_fields/<widget>/_field.gsp`
        def widget = getWidget(propertyAccessor.constraints)
        if (widget) {
            templateResolveOrder << appendPiecesForUri("/_fields", widget, templateName)
        }

        // if we have a property type look in `grails-app/views/_fields/<propertyType>/_field.gsp` and equivalent for superclasses
        if (propertyAccessor.propertyType) {
            templateResolveOrder << appendPiecesForUri("/_fields", toPropertyNameFormat(propertyAccessor.propertyType), templateName)
            for (propertySuperClass in propertyAccessor.propertyTypeSuperclasses) {
                templateResolveOrder << appendPiecesForUri("/_fields", toPropertyNameFormat(propertySuperClass), templateName)
            }
        }

        // if nothing else is found fall back to a default (even this may not exist for f:input)
        templateResolveOrder << "/_fields/default/$templateName"

        templateResolveOrder
    }

    private String getAssociationPath(BeanPropertyAccessor propertyAccessor) {
        def associationPath = null
        if (propertyAccessor.persistentProperty?.oneToOne) associationPath = 'oneToOne'
        if (propertyAccessor.persistentProperty?.oneToMany) associationPath = 'oneToMany'
        if (propertyAccessor.persistentProperty?.manyToMany) associationPath = 'manyToMany'
        if (propertyAccessor.persistentProperty?.manyToOne) associationPath = 'manyToOne'
        associationPath
    }

    private String getWidget(ConstrainedProperty cp) {
        if (cp.widget) {
            return cp.widget
        }
        if (cp.password) {
            return 'password'
        }
        if (CharSequence.isAssignableFrom(cp.propertyType)) {
            if (cp.url) {
                return 'url'
            }
            if (cp.creditCard) {
                return 'creditCard'
            }
            if (cp.email) {
                return 'email'
            }
        }
        null
    }

    private String getControllerName() {
        RequestContextHolder.requestAttributes?.controllerName
    }

    private String getActionName() {
        RequestContextHolder.requestAttributes?.actionName
    }

    private boolean shouldCache() {
        // If not explicitely specified, there is no template caching
        Boolean cacheDisabled = grailsApplication?.config?.grails?.plugin?.fields?.disableLookupCache
        return !cacheDisabled
    }

}
