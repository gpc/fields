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

import grails.core.GrailsApplication
import grails.core.GrailsDomainClass
import grails.core.GrailsDomainClassProperty
import grails.core.support.GrailsApplicationAware
import groovy.xml.MarkupBuilder
import org.apache.commons.lang.StringUtils
import org.grails.buffer.FastStringWriter
import org.grails.gsp.GroovyPage
import org.grails.validation.DomainClassPropertyComparator

import java.sql.Blob

import static FormFieldsTemplateService.toPropertyNameFormat
import static grails.util.GrailsClassUtils.getStaticPropertyValue

class FormFieldsTagLib implements GrailsApplicationAware {

	static final namespace = 'f'
    static final String STACK_PAGE_SCOPE_VARIABLE = 'f:with:stack'

	FormFieldsTemplateService formFieldsTemplateService
	GrailsApplication grailsApplication
	BeanPropertyAccessorFactory beanPropertyAccessorFactory

	static defaultEncodeAs = [taglib:'raw']

    class BeanAndPrefix {
        Object bean
        String prefix
    }

    class BeanAndPrefixStack extends Stack<BeanAndPrefix> {
        Object getBean() {
            empty() ? null : peek().bean
        }

        String getPrefix() {
            empty() ? null : peek().prefix
        }

        String toString() {
            bean?.toString() ?: ''
        }
    }

    BeanAndPrefixStack getBeanStack() {
        if (!pageScope.hasVariable(STACK_PAGE_SCOPE_VARIABLE)) {
            pageScope.setVariable(STACK_PAGE_SCOPE_VARIABLE, new BeanAndPrefixStack())
        }
        pageScope.variables[STACK_PAGE_SCOPE_VARIABLE]
    }

	/**
	 * @attr bean REQUIRED Name of the source bean in the GSP model.
	 * @attr prefix Prefix to add to input element names.
	 */
	def with = { attrs, body ->
		if (!attrs.bean) throwTagError("Tag [with] is missing required attribute [bean]")
        BeanAndPrefix beanPrefix = resolveBeanAndPrefix(attrs.bean, attrs.prefix)
		try {
            beanStack.push(beanPrefix)
			out << body()
		} finally {
            beanStack.pop()
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
	 * @attr wrapper Specify the folder inside _fields where to look up for the wrapper template.
	 * @attr widget Specify the folder inside _fields where to look up for the widget template.
	 * @attr templates Specify the folder inside _fields where to look up for the wrapper and widget template.
	 */
	def field = { attrs, body ->
		if (attrs.containsKey('bean') && !attrs.bean) throwTagError("Tag [field] requires a non-null value for attribute [bean]")
		if (!attrs.property) throwTagError("Tag [field] is missing required attribute [property]")

		def bean = resolveBean(attrs.remove('bean'))
		def property = attrs.remove('property')
        def templatesFolder = attrs.remove('templates')
        def fieldFolder = attrs.remove('wrapper')
        def widgetFolder = attrs.remove('widget')

		def propertyAccessor = resolveProperty(bean, property)
		if (propertyAccessor.persistentProperty?.embedded) {
			renderEmbeddedProperties(bean, propertyAccessor, attrs + [templates: templatesFolder, field: fieldFolder, input: widgetFolder])
		} else {
			def model = buildModel(propertyAccessor, attrs)
			def wrapperAttrs = [:]
			def widgetAttrs = [:]

            String prefixAttribute = formFieldsTemplateService.getWidgetPrefix() ?: 'widget-'
			attrs.each { k, v ->
				
				if (k?.startsWith(prefixAttribute))
					widgetAttrs[k.replace(prefixAttribute, '')] = v
				else
					wrapperAttrs[k] = v
			}

			if (hasBody(body)) {
                model.widget = raw(body(model + [attrs: widgetAttrs] + widgetAttrs))
            } else {
				model.widget = renderWidget(propertyAccessor, model, widgetAttrs, widgetFolder?:templatesFolder)
			}


			def templateName = formFieldsTemplateService.getTemplateFor("wrapper")
			def template = formFieldsTemplateService.findTemplate(propertyAccessor, templateName, fieldFolder?:templatesFolder)
			if (template) {
				out << render(template: template.path, plugin: template.plugin, model: model + [attrs: wrapperAttrs] + wrapperAttrs)
			} else {
                out << renderDefaultField(model)
			}
		}
	}
    /**
      * Renders a collection of beans in a table
      *
      * @attr collection REQUIRED The collection of beans to render
      * @attr domainClass The FQN of the domain class of the elements in the collection.
      * Defaults to the class of the first element in the collection.
      * @attr properties The list of properties to be shown (table columns).
      * Defaults to the first 7 (or less) properties of the domain class ordered by the domain class' constraints.
      * @attr displayStyle OPTIONAL Determines the display template used for the bean's properties.
      * Defaults to 'table', meaning that 'display-table' templates will be used when available.
      */
    def table = { attrs, body ->
        def collection = resolveBean(attrs.remove('collection'))
        def domainClass
        if (attrs.containsKey('domainClass')) {
            domainClass = grailsApplication.getDomainClass(attrs.remove('domainClass'))
        } else {
            domainClass = (collection instanceof Collection) && collection ? resolveDomainClass(collection.iterator().next()) : null
        }
        if (domainClass) {
         def properties
         if (attrs.containsKey('properties')) {
            properties = attrs.remove('properties').collect { domainClass.getPropertyByName(it) }
         } else {
            properties = domainClass.persistentProperties.sort(new DomainClassPropertyComparator(domainClass))
            if (properties.size() > 6) {
                properties = properties[0..6]
            }
         }
         def displayStyle = attrs.remove('displayStyle')
         out << render(template: "/templates/_fields/table",
                 model: [domainClass: domainClass, domainProperties: properties, collection: collection, displayStyle: displayStyle])
        }
    }
	/**
	 * @deprecated since version 1.5 - Use widget instead
	 * @attr bean Name of the source bean in the GSP model.
	 * @attr property REQUIRED The name of the property to display. This is resolved
	 * against the specified bean or the bean in the current scope.
	 */
	def input = { attrs ->
		out << widget(attrs)
	}

	/**
	 * @attr bean Name of the source bean in the GSP model.
	 * @attr property REQUIRED The name of the property to display. This is resolved
	 * against the specified bean or the bean in the current scope.
	 */
	def widget = { attrs ->
		def bean = resolveBean(attrs.remove('bean'))
		if (!bean) throwTagError("Tag [input] is missing required attribute [bean]")
		if (!attrs.property) throwTagError("Tag [input] is missing required attribute [property]")

		def property = attrs.remove('property')
        def widgetFolder = attrs.remove('widget')

		def propertyAccessor = resolveProperty(bean, property)
		def model = buildModel(propertyAccessor, attrs)

		out << renderWidget(propertyAccessor, model, attrs, widgetFolder)
	}

	/**
	 * @attr bean Name of the source bean in the GSP model.
	 * @attr property REQUIRED The name of the property to display. This is resolved
	 * against the specified bean or the bean in the current scope.
	 */
	def displayWidget = { attrs ->
		def bean = resolveBean(attrs.remove('bean'))
		if (!bean) throwTagError("Tag [displayWidget] is missing required attribute [bean]")
		if (!attrs.property) throwTagError("Tag [displayWidget] is missing required attribute [property]")

		def property = attrs.remove('property')
        def widgetFolder = attrs.remove('widget')

		def propertyAccessor = resolveProperty(bean, property)
		def model = buildModel(propertyAccessor, attrs)

		out << renderDisplayWidget(propertyAccessor, model, attrs, widgetFolder)
	}

	/**
	 * @attr bean Name of the source bean in the GSP model.
	 * @attr property REQUIRED The name of the property to display. This is resolved
	 * against the specified bean or the bean in the current scope.
	 */
	def display = { attrs, body ->
		def bean = resolveBean(attrs.remove('bean'))
		if (!bean) throwTagError("Tag [display] is missing required attribute [bean]")

		def property = attrs.remove('property')
		def templatesFolder = attrs.remove('templates')

		if (property == null) {
			GrailsDomainClass domainClass = resolveDomainClass(bean)
			if (domainClass) {
				def properties = domainClass.persistentProperties.sort(new DomainClassPropertyComparator(domainClass))
				out << render(template: "/templates/_fields/list", model: [domainClass: domainClass, domainProperties: properties]) { prop ->
					def propertyAccessor = resolveProperty(bean, prop.name)
					def model = buildModel(propertyAccessor, attrs)
					out << raw(renderDisplayWidget(propertyAccessor, model, attrs,templatesFolder))
				}
			}
		} else {
            def displayFolder = attrs.remove('wrapper')
            def widgetFolder = attrs.remove('widget')

            def propertyAccessor = resolveProperty(bean, property)
            def model = buildModel(propertyAccessor, attrs)

            def wrapperAttrs = [:]
            def widgetAttrs = [:]

            String prefixAttribute = formFieldsTemplateService.getWidgetPrefix() ?: 'widget-'
            attrs.each { k, v ->            
                if (k?.startsWith(prefixAttribute))
                    widgetAttrs[k.replace(prefixAttribute, '')] = v
                else
                    wrapperAttrs[k] = v
            }


			def wrapperFolderTouse = displayFolder ?: templatesFolder
			def widgetsFolderToUse = widgetFolder ?: templatesFolder
			if (hasBody(body)) {
                model.widget = raw(body(model + [attrs: widgetAttrs] + widgetAttrs))
                model.value = body(model)
            } else {
                model.widget = renderDisplayWidget(propertyAccessor, model, widgetAttrs, widgetsFolderToUse)
            }


			def displayWrapperTemplateName = formFieldsTemplateService.getTemplateFor("displayWrapper")
			def template = formFieldsTemplateService.findTemplate(propertyAccessor, displayWrapperTemplateName, wrapperFolderTouse)
            if (template) {
                out << render(template: template.path, plugin: template.plugin, model: model + [attrs: wrapperAttrs] + wrapperAttrs)
            } else {
				out << raw(renderDisplayWidget(propertyAccessor, model, attrs, widgetsFolderToUse))
            }
        }

	}

    private void renderEmbeddedProperties(bean, BeanPropertyAccessor propertyAccessor, attrs) {
		def legend = resolveMessage(propertyAccessor.labelKeys, propertyAccessor.defaultLabel)
		out << applyLayout(name: '_fields/embedded', params: [type: toPropertyNameFormat(propertyAccessor.propertyType), legend: legend]) {
			for (embeddedProp in resolvePersistentProperties(propertyAccessor.persistentProperty.component, attrs)) {
				def propertyPath = "${propertyAccessor.pathFromRoot}.${embeddedProp.name}"
				out << field(bean: bean, property: propertyPath, prefix: attrs.prefix, default: attrs.default, field: attrs.field, widget: attrs.widget)
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

    private CharSequence renderWidget(BeanPropertyAccessor propertyAccessor, Map model, Map attrs = [:], String widgetFolder) {
		def widgetTemplateName = formFieldsTemplateService.getTemplateFor("widget")
		def template = formFieldsTemplateService.findTemplate(propertyAccessor, widgetTemplateName, widgetFolder)
		if (template) {
			render template: template.path, plugin: template.plugin, model: model + [attrs: attrs] + attrs
		} else {
			renderDefaultInput model, attrs
		}
	}

	private CharSequence renderDisplayWidget(BeanPropertyAccessor propertyAccessor, Map model, Map attrs = [:], String widgetFolder) {
		def displayStyle = attrs.displayStyle

		def template = null
		if (displayStyle && displayStyle != 'default') {
			template = formFieldsTemplateService.findTemplate(propertyAccessor, "displayWidget-$attrs.displayStyle", widgetFolder)
		}
		if (!template) {
			def displayWidgetTemplateName = formFieldsTemplateService.getTemplateFor("displayWidget")
			template = formFieldsTemplateService.findTemplate(propertyAccessor, displayWidgetTemplateName, widgetFolder)
		}

		if (template) {
			render template: template.path, plugin: template.plugin, model: model + [attrs: attrs] + attrs
		} else if (!(model.value instanceof CharSequence)) {
			renderDefaultDisplay(model, attrs, widgetFolder)
		} else {
			model.value
		}
	}

    private resolvePageScopeVariable(attributeName) {
        // Tomcat throws NPE if you query pageScope for null/empty values
        attributeName?.toString() ? pageScope.variables[attributeName] : null
    }

    private BeanAndPrefix resolveBeanAndPrefix(beanAttribute, prefixAttribute) {
        def bean = resolvePageScopeVariable(beanAttribute) ?: beanAttribute
        def prefix = resolvePageScopeVariable(prefixAttribute) ?: prefixAttribute
        new BeanAndPrefix(bean: bean, prefix: prefix)
    }

	private Object resolveBean(beanAttribute) {
        resolvePageScopeVariable(beanAttribute) ?: beanAttribute ?: beanStack.bean
	}

    private String resolvePrefix(prefixAttribute) {
        def prefix = resolvePageScopeVariable(prefixAttribute) ?: prefixAttribute ?: beanStack.prefix
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
        def properties

        if(attrs.order) {
            if(attrs.except) {
                throwTagError('The [except] and [order] attributes may not be used together.')
            }
            def orderBy = attrs.order?.tokenize(',')*.trim() ?: []
            properties = orderBy.collect { propertyName -> domainClass.getPersistentProperty(propertyName) }
        } else {
            properties = domainClass.persistentProperties as List
            def blacklist = attrs.except?.tokenize(',')*.trim() ?: []
            blacklist << 'dateCreated' << 'lastUpdated'
            def scaffoldProp = getStaticPropertyValue(domainClass.clazz, 'scaffold')
            if (scaffoldProp) {
                blacklist.addAll(scaffoldProp.exclude)
            }
            properties.removeAll { it.name in blacklist }
            properties.removeAll { !it.domainClass.constrainedProperties[it.name]?.display }
            properties.removeAll { it.derived }

            Collections.sort(properties, new org.grails.validation.DomainClassPropertyComparator(domainClass))
        }
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
            def widget = model.widget
            if(widget != null) {
                mkp.yieldUnescaped widget    
            }
			
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
            } else if (model.constraints?.password) {
                attrs.type = "password"
                attrs.remove('value')
            } else if (model.constraints?.email) attrs.type = "email"
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

        if (model.constraints?.inList) {
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

    private CharSequence renderDefaultDisplay(Map model, Map attrs = [:], String templatesFolder) {
        def persistentProperty = model.persistentProperty
        if (persistentProperty?.association) {
            if (persistentProperty.embedded) {
                return (attrs.displayStyle == 'table') ? model.value?.toString().encodeAsHTML() :
                        displayEmbedded(model.value, persistentProperty.component, attrs, templatesFolder)
            } else if (persistentProperty.oneToMany || persistentProperty.manyToMany) {
                return displayAssociationList(model.value, persistentProperty.referencedDomainClass)
            } else {
                return displayAssociation(model.value, persistentProperty.referencedDomainClass)
            }
        }

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

    private CharSequence displayEmbedded(bean, GrailsDomainClass domainClass, Map attrs, String templatesFolder) {
        def buffer = new FastStringWriter()
        def properties = domainClass.persistentProperties.sort(new DomainClassPropertyComparator(domainClass))
        buffer << render(template: "/templates/_fields/list", model: [domainClass: domainClass, domainProperties: properties]) { prop ->
            def propertyAccessor = resolveProperty(bean, prop.name)
            def model = buildModel(propertyAccessor, attrs)
            out << raw(renderDisplayWidget(propertyAccessor, model, attrs, templatesFolder))
        }
        buffer.buffer
    }

    private CharSequence displayAssociationList(values, GrailsDomainClass referencedDomainClass) {
        def buffer = new FastStringWriter()
        buffer << '<ul>'
        for (value in values) {
            buffer << '<li>'
            buffer << displayAssociation(value, referencedDomainClass)
            buffer << '</li>'
        }
        buffer << '</ul>'
        buffer.buffer
    }

    private CharSequence displayAssociation(value, GrailsDomainClass referencedDomainClass) {
        if(value) {
            g.link(controller: referencedDomainClass.propertyName, action: "show", id: value.id, value.toString().encodeAsHTML())    
        }        
    }

    private boolean isEditable(constraints) {
        !constraints || constraints.editable
    }
}
