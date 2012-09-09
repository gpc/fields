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

    boolean ignoreControllerLookup = false
    boolean ignoreControllerActionLookup = false
    boolean ignoreAssociationLookup = false
    boolean ignoreBeanTypeLookup = false
    boolean ignoreBeanSuperTypeLookup = false
    boolean ignorePropertyTypeLookup = false
    boolean ignorePropertySuperTypeLookup = false

    Map findTemplate(BeanPropertyAccessor propertyAccessor, String templateName) {
        findTemplateCached(propertyAccessor, controllerName, actionName, templateName)
    }

    private final Closure findTemplateCached = shouldCache() ? this.&findTemplateCacheable.memoize() : this.&findTemplateCacheable

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
        GrailsNameUtils.getLogicalPropertyName(type.name, '')
    }

    private List<String> candidateTemplatePaths(BeanPropertyAccessor propertyAccessor, String controllerName, String actionName, String templateName) {
        def templateResolveOrder = []

        // if there is a controller for the current request any template in its views directory takes priority
        if (!ignoreControllerLookup && controllerName) {
            // first try action-specific templates
            if (!ignoreControllerActionLookup) appendControllerActionTemplates(templateResolveOrder, controllerName, actionName, propertyAccessor, templateName)

            // then general templates for the controller
            appendControllerTemplates(templateResolveOrder, controllerName, propertyAccessor, templateName)
        }

        // if we have a bean type look in `grails-app/views/_fields/<beanType>/<propertyName>/_field.gsp` and equivalent for superclasses
        if (!ignoreBeanTypeLookup && propertyAccessor.beanType) {
            appendBeanTypeTemplates(templateResolveOrder, propertyAccessor, templateName)
            if (!ignoreBeanSuperTypeLookup) appendBeanSuperTypeTemplates(templateResolveOrder, propertyAccessor, templateName)
        }

        // if this is an association property look in `grails-app/views/_fields/<associationType>/_field.gsp`
        if (!ignoreAssociationLookup) appendAssociationTemplates(templateResolveOrder, propertyAccessor, templateName)
        // if we have a property type look in `grails-app/views/_fields/<propertyType>/_field.gsp` and equivalent for superclasses
        if (!ignorePropertyTypeLookup && propertyAccessor.propertyType) {
            appendPropertyTypeTemplates(templateResolveOrder, propertyAccessor, templateName)
            if (!ignorePropertySuperTypeLookup) appendPropertySuperTypeTemplates(templateResolveOrder, propertyAccessor, templateName)
        }

        // if nothing else is found fall back to a default (even this may not exist for f:input)
        templateResolveOrder << "/_fields/default/$templateName"

        templateResolveOrder
    }

    protected void appendControllerTemplates(ArrayList templateResolveOrder, String controllerName, BeanPropertyAccessor propertyAccessor, String templateName) {
        templateResolveOrder << appendPiecesForUri("/", controllerName, propertyAccessor.propertyName, templateName)
        if (propertyAccessor.propertyType) templateResolveOrder << appendPiecesForUri("/", controllerName, toPropertyNameFormat(propertyAccessor.propertyType), templateName)
        templateResolveOrder << appendPiecesForUri("/", controllerName, templateName)
    }

    protected void appendControllerActionTemplates(ArrayList templateResolveOrder, String controllerName, String actionName, BeanPropertyAccessor propertyAccessor, String templateName) {
        templateResolveOrder << appendPiecesForUri("/", controllerName, actionName, propertyAccessor.propertyName, templateName)
        if (propertyAccessor.propertyType) templateResolveOrder << appendPiecesForUri("/", controllerName, actionName, toPropertyNameFormat(propertyAccessor.propertyType), templateName)
        templateResolveOrder << appendPiecesForUri("/", controllerName, actionName, templateName)
    }

    protected void appendBeanTypeTemplates(ArrayList templateResolveOrder, BeanPropertyAccessor propertyAccessor, String templateName) {
        templateResolveOrder << appendPiecesForUri("/_fields", toPropertyNameFormat(propertyAccessor.beanType), propertyAccessor.propertyName, templateName)
    }

    protected void appendBeanSuperTypeTemplates(ArrayList templateResolveOrder, BeanPropertyAccessor propertyAccessor, String templateName) {
        for (superclass in propertyAccessor.beanSuperclasses) {
            templateResolveOrder << appendPiecesForUri("/_fields", toPropertyNameFormat(superclass), propertyAccessor.propertyName, templateName)
        }
    }

    protected void appendAssociationTemplates(ArrayList templateResolveOrder, BeanPropertyAccessor propertyAccessor, String templateName) {
        def associationPath = getAssociationPath(propertyAccessor)
        if (associationPath) {
            templateResolveOrder << appendPiecesForUri('/_fields', associationPath, templateName)
        }
    }

    protected void appendPropertyTypeTemplates(ArrayList templateResolveOrder, BeanPropertyAccessor propertyAccessor, String templateName) {
        templateResolveOrder << appendPiecesForUri("/_fields", toPropertyNameFormat(propertyAccessor.propertyType), templateName)
    }

    protected void appendPropertySuperTypeTemplates(ArrayList templateResolveOrder, BeanPropertyAccessor propertyAccessor, String templateName) {
        for (propertySuperClass in propertyAccessor.propertyTypeSuperclasses) {
            templateResolveOrder << appendPiecesForUri("/_fields", toPropertyNameFormat(propertySuperClass), templateName)
        }
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
