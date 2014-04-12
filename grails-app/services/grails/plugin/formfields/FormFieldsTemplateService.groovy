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

import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.springframework.web.context.request.RequestContextHolder
import grails.util.*
import static grails.util.Environment.DEVELOPMENT
import static org.codehaus.groovy.grails.io.support.GrailsResourceUtils.appendPiecesForUri

class FormFieldsTemplateService {

    static transactional = false

    GrailsConventionGroovyPageLocator groovyPageLocator
    GrailsPluginManager pluginManager

    Map findTemplate(BeanPropertyAccessor propertyAccessor, String templateName, String componentName = "") {
        findTemplateCached.call(propertyAccessor, controllerName, actionName, templateName, componentName)
    }

    private final Closure findTemplateCached = shouldCache() ? this.&findTemplateCacheable.memoize() : this.&findTemplateCacheable
    public final Closure findTemplateByPathCached = shouldCache() ? this.&findTemplateByPathCacheable.memoize() : this.&findTemplateByPathCacheable

    private Map findTemplateCacheable(BeanPropertyAccessor propertyAccessor, String controllerName, String actionName, String templateName, String componentName) {
        def candidatePaths = candidateTemplatePaths(propertyAccessor, controllerName, actionName, templateName, componentName)

        candidatePaths.findResult { path ->
            return findTemplateByPathCached.call(path)
        }
    }

    public Map findTemplateByPathCacheable(String path){
        log.debug "looking for template with path $path"
        def source = groovyPageLocator.findTemplateByPath(path)
        def template = null
        if (source) {
            template = [path:path]
            def plugin = pluginManager.allPlugins.find {
                source.URI.startsWith(it.pluginPath)
            }
            template.plugin = plugin?.name
            log.info "found template $template.path ${plugin ? "in $template.plugin plugin" : ''}"
        } else {
            log.info "template $path was not found"
        }
        return template
    }

    static String toPropertyNameFormat(Class type) {
        GrailsNameUtils.getLogicalPropertyName(type.name, '')
    }

    private List<String> candidateTemplatePaths(BeanPropertyAccessor propertyAccessor, String controllerName, String actionName, String templateName, String componentName) {
        def templateResolveOrder = []

        //if there is a component name then takes priority
        if(componentName != ""){
            templateResolveOrder << appendPiecesForUri("/_fields/_components", componentName, templateName)
        }

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

    private String getControllerName() {
        RequestContextHolder.requestAttributes?.controllerName
    }

    private String getActionName() {
        RequestContextHolder.requestAttributes?.actionName
    }

    private static boolean shouldCache() {
        Environment.current != DEVELOPMENT
    }

}

