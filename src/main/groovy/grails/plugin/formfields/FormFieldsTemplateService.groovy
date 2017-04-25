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
import grails.core.GrailsApplication
import grails.plugins.GrailsPluginManager
import grails.validation.ConstrainedProperty
import groovy.util.logging.Slf4j
import org.grails.web.gsp.io.GrailsConventionGroovyPageLocator
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

import static org.grails.io.support.GrailsResourceUtils.appendPiecesForUri

@Slf4j
class FormFieldsTemplateService {
    public static final String SETTING_WIDGET_PREFIX = 'grails.plugin.fields.widgetPrefix'
    private static final String THEMES_FOLDER = "_themes"

    GrailsApplication grailsApplication

    GrailsConventionGroovyPageLocator groovyPageLocator
    GrailsPluginManager pluginManager

    String getWidgetPrefix(){
        Closure widgetPrefixNameResolver = getWidgetPrefixName
        return widgetPrefixNameResolver()
    }

    @Lazy
    private Closure getWidgetPrefixName = shouldCache() ? getWidgetPrefixNameCacheable.memoize() : getWidgetPrefixNameCacheable

    private Closure getWidgetPrefixNameCacheable = { ->
        return grailsApplication?.config?.getProperty(SETTING_WIDGET_PREFIX, 'widget-')
    }

    Map findTemplate(BeanPropertyAccessor propertyAccessor, String templateName, String templatesFolder, String theme = null) {
        // it looks like the assignment below is redundant, but tests fail if findTemplateCached is invoked directly
        Closure templateFinder = findTemplateCached
        templateFinder(propertyAccessor, controllerNamespace, controllerName, actionName, templateName, templatesFolder, theme)
    }

    String getTemplateFor(String property){
        Closure nameFinder = getTemplateName
        nameFinder(property)
    }

    @Lazy
    private Closure getTemplateName = shouldCache() ? getTemplateNameCacheable.memoize() : getTemplateNameCacheable

    private getTemplateNameCacheable = { String templateProperty ->
        return grailsApplication?.config?.getProperty("grails.plugin.fields.$templateProperty", templateProperty) ?: templateProperty
    }

    @Lazy
    private Closure findTemplateCached = shouldCache() ? findTemplateCacheable.memoize() : findTemplateCacheable

    private findTemplateCacheable = { BeanPropertyAccessor propertyAccessor, String controllerNamespace, String controllerName, String actionName, String templateName, String templatesFolder, String themeName ->
        List<String> candidatePaths
        if(themeName) {
            //if theme is specified, first resolve all theme paths and then all the default paths
            String themeFolder = THEMES_FOLDER + "/" + themeName
            candidatePaths = candidateTemplatePaths(propertyAccessor, controllerNamespace, controllerName, actionName, templateName, templatesFolder, themeFolder)
            candidatePaths = candidatePaths + candidateTemplatePaths(propertyAccessor, controllerNamespace, controllerName, actionName, templateName, templatesFolder, null)
        } else {
            candidatePaths = candidateTemplatePaths(propertyAccessor, controllerNamespace, controllerName, actionName, templateName, templatesFolder, null)
        }

        candidatePaths.findResult {String path ->
            FormFieldsTemplateService.log.debug "looking for template with path $path"
            def source = groovyPageLocator.findTemplateByPath(path)
            if (source) {
                Map template = [path: path]
                def plugin = pluginManager.allPlugins.find {
                    source.URI.startsWith(it.pluginPath)
                }
                template.plugin = plugin?.name
                FormFieldsTemplateService.log.info "found template $template.path ${plugin ? "in $template.plugin plugin" : ''}"
                return template
            } else {
                null
            }
        }
    }

    static String toPropertyNameFormat(Class type) {
        def propertyNameFormat = GrailsNameUtils.getLogicalPropertyName(type.canonicalName, '')
        if (propertyNameFormat.endsWith('[]')) {
            propertyNameFormat = propertyNameFormat - '[]' + 'Array'
        }
        return propertyNameFormat
    }

    private List<String> candidateTemplatePaths(BeanPropertyAccessor propertyAccessor, String controllerNamespace, String controllerName, String actionName, String templateName, String templatesFolder, String themeFolder) {
        List<String> templateResolveOrder = []

        // if we have a widget look in `grails-app/views/_fields/<templateFolder>/_field.gsp`
        if (templatesFolder) {
            templateResolveOrder << appendPiecesForUri("/_fields", themeFolder, templatesFolder, templateName)
        }

        // if there is a controller namespace for the current request any template in its views directory takes priority
        if (controllerNamespace) {
            // first try action-specific templates
            templateResolveOrder << appendPiecesForUri("/", controllerNamespace, controllerName, actionName, propertyAccessor.propertyName, themeFolder, templateName)
            if (propertyAccessor.propertyType) templateResolveOrder << appendPiecesForUri("/", controllerNamespace, controllerName, actionName, themeFolder, toPropertyNameFormat(propertyAccessor.propertyType), templateName)
            templateResolveOrder << appendPiecesForUri("/", controllerNamespace, controllerName, actionName, themeFolder, templateName)

            // then general templates for the controller
            templateResolveOrder << appendPiecesForUri("/", controllerNamespace, controllerName, propertyAccessor.propertyName, themeFolder, templateName)
            if (propertyAccessor.propertyType) templateResolveOrder << appendPiecesForUri("/", controllerNamespace, controllerName, themeFolder, toPropertyNameFormat(propertyAccessor.propertyType), templateName)
            templateResolveOrder << appendPiecesForUri("/", controllerNamespace, controllerName, themeFolder, templateName)
        }

        // if there is a controller for the current request any template in its views directory takes priority
        if (controllerName) {
            // first try action-specific templates
            templateResolveOrder << appendPiecesForUri("/", controllerName, actionName, propertyAccessor.propertyName, themeFolder, templateName)
            if (propertyAccessor.propertyType) templateResolveOrder << appendPiecesForUri("/", controllerName, actionName, themeFolder, toPropertyNameFormat(propertyAccessor.propertyType), templateName)
            templateResolveOrder << appendPiecesForUri("/", controllerName, actionName, themeFolder, templateName)

            // then general templates for the controller
            templateResolveOrder << appendPiecesForUri("/", controllerName, propertyAccessor.propertyName, themeFolder, templateName)
            if (propertyAccessor.propertyType) templateResolveOrder << appendPiecesForUri("/", controllerName, themeFolder, toPropertyNameFormat(propertyAccessor.propertyType), templateName)
            templateResolveOrder << appendPiecesForUri("/", controllerName, themeFolder, templateName)
        }

        // if we have a bean type look in `grails-app/views/_fields/<beanType>/<propertyName>/_field.gsp` and equivalent for superclasses
        if (propertyAccessor.beanType) {
            templateResolveOrder << appendPiecesForUri("/_fields", themeFolder, toPropertyNameFormat(propertyAccessor.beanType), propertyAccessor.propertyName, templateName)
            for (superclass in propertyAccessor.beanSuperclasses) {
                templateResolveOrder << appendPiecesForUri("/_fields", themeFolder, toPropertyNameFormat(superclass), propertyAccessor.propertyName, templateName)
            }
        }

        // if this is an association property look in `grails-app/views/_fields/<associationType>/_field.gsp`
        String associationPath = getAssociationPath(propertyAccessor)
        if (associationPath) {
            templateResolveOrder << appendPiecesForUri('/_fields', themeFolder, associationPath, templateName)
        }

        // if we have a domain constraint widget look in `grails-app/views/_fields/<widget>/_field.gsp`
        String widget = getWidget(propertyAccessor.constraints)
        if (widget) {
            templateResolveOrder << appendPiecesForUri("/_fields", themeFolder, widget, templateName)
        }

        // if we have a property type look in `grails-app/views/_fields/<propertyType>/_field.gsp` and equivalent for superclasses
        if (propertyAccessor.propertyType) {
            templateResolveOrder << appendPiecesForUri("/_fields", themeFolder, toPropertyNameFormat(propertyAccessor.propertyType), templateName)
            for (propertySuperClass in propertyAccessor.propertyTypeSuperclasses) {
                templateResolveOrder << appendPiecesForUri("/_fields", themeFolder, toPropertyNameFormat(propertySuperClass), templateName)
            }
        }

        // if nothing else is found fall back to a default (even this may not exist for f:input)
        templateResolveOrder << appendPiecesForUri("/_fields", themeFolder, "default", templateName)

        templateResolveOrder
    }

    private String getAssociationPath(BeanPropertyAccessor propertyAccessor) {
        String associationPath = null
        if (propertyAccessor.persistentProperty?.oneToOne) associationPath = 'oneToOne'
        if (propertyAccessor.persistentProperty?.oneToMany) associationPath = 'oneToMany'
        if (propertyAccessor.persistentProperty?.manyToMany) associationPath = 'manyToMany'
        if (propertyAccessor.persistentProperty?.manyToOne) associationPath = 'manyToOne'
        associationPath
    }

    protected String getWidget(ConstrainedProperty cp) {
        if (null == cp) {
            return null
        }
        String widget = null
        if (cp.widget) {
            widget = cp.widget
        } else if (cp.password) {
            widget = 'password'
        } else if (CharSequence.isAssignableFrom(cp.propertyType)) {
            if (cp.url) {
                widget = 'url'
            } else if (cp.creditCard) {
                widget = 'creditCard'
            } else if (cp.email) {
                widget = 'email'
            }
        }
        return widget
    }

    private String getControllerNamespace() {
        if (GrailsWebRequest.metaClass.respondsTo(GrailsWebRequest, "getControllerNamespace").size() > 0) {
            return RequestContextHolder.requestAttributes?.getAttribute(GrailsApplicationAttributes.CONTROLLER_NAMESPACE_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)
        }
    }

    private String getControllerName() {
        RequestContextHolder.requestAttributes?.controllerName
    }

    private String getActionName() {
        RequestContextHolder.requestAttributes?.actionName
    }

    private boolean shouldCache() {
        // If not explicitly specified, there is no template caching
        Boolean cacheDisabled = grailsApplication?.config?.grails?.plugin?.fields?.disableLookupCache
        return !cacheDisabled
    }

}