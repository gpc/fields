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
import grails.validation.Constrained
import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder
import org.apache.commons.lang.StringUtils
import org.grails.buffer.FastStringWriter
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.*
import org.grails.gsp.GroovyPage
import org.grails.scaffolding.model.DomainModelService
import org.grails.scaffolding.model.DomainModelServiceImpl
import org.grails.scaffolding.model.property.DomainPropertyFactory

import java.sql.Blob

import static FormFieldsTemplateService.toPropertyNameFormat

class FormFieldsTagLib implements GrailsApplicationAware {
	static final namespace = 'f'

    static final String STACK_PAGE_SCOPE_VARIABLE = 'f:with:stack'
	private static final List<String> DECIMAL_TYPES = ['double', 'float', 'bigdecimal']

	private static final String THEME_ATTR = "theme"
	private static final String WIDGET_ATTR = "widget"
	private static final String WRAPPER_ATTR = "wrapper"
	private static final String TEMPLATES_ATTR = "templates"
	private  static final String PROPERTY_ATTR = "property"
	private static final String BEAN_ATTR = "bean"

	FormFieldsTemplateService formFieldsTemplateService
	GrailsApplication grailsApplication
	BeanPropertyAccessorFactory beanPropertyAccessorFactory
	DomainPropertyFactory fieldsDomainPropertyFactory
	MappingContext grailsDomainClassMappingContext

	private DomainModelService _domainModelService

	protected DomainModelService getDomainModelService() {
		if (!_domainModelService) {
			_domainModelService = new DomainModelServiceImpl(domainPropertyFactory: fieldsDomainPropertyFactory)
		}
		_domainModelService
	}

	static defaultEncodeAs = [taglib:'raw']

    class BeanAndPrefix {
        Object bean
        String prefix
		Map<String, Object> innerAttributes = [:]
    }

    class BeanAndPrefixStack extends Stack<BeanAndPrefix> {
        Object getBean() {
            empty() ? null : peek().bean
        }

        String getPrefix() {
            empty() ? null : peek().prefix
        }

		Map<String, Object> getInnerAttributes() {
			empty() ? [:] : peek().innerAttributes
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
		if (!attrs[BEAN_ATTR]) throwTagError("Tag [with] is missing required attribute [$BEAN_ATTR]")
        BeanAndPrefix beanPrefix = resolveBeanAndPrefix(attrs[BEAN_ATTR], attrs.prefix, attrs)
		try {
            beanStack.push(beanPrefix)
			out << body()
		} finally {
            beanStack.pop()
		}
	}

	/**
	 * @attr bean REQUIRED Name of the source bean in the GSP model.
	 * @attr order A comma-separated list of properties to include in provided order
	 * @attr except A comma-separated list of properties to exclude from the generated list of input fields
	 * @attr prefix Prefix to add to input element names.
	 */
	def all = { attrs ->
		if (!attrs[BEAN_ATTR]) throwTagError("Tag [all] is missing required attribute [$BEAN_ATTR]")
		BeanAndPrefix beanPrefix = resolveBeanAndPrefix(attrs[BEAN_ATTR], attrs.prefix, attrs)
		try {
			beanStack.push(beanPrefix)
			def bean = resolveBean(attrs[BEAN_ATTR])
			def prefix = resolvePrefix(attrs.prefix)
			def domainClass = resolveDomainClass(bean)
			if (domainClass) {
				for (property in resolvePersistentProperties(domainClass, attrs)) {
					out << field([bean: bean, property: property.name, prefix: prefix])
				}
			} else {
				throwTagError('Tag [all] currently only supports domain types')
			}
		} finally {
			beanStack.pop()
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
	 * @attr theme Theme name
	 */
	def field = { attrs, body ->
		attrs = beanStack.innerAttributes + attrs
		if (attrs.containsKey(BEAN_ATTR) && !attrs[BEAN_ATTR]) throwTagError("Tag [field] requires a non-null value for attribute [$BEAN_ATTR]")
		if (!attrs[PROPERTY_ATTR]) throwTagError("Tag [field] is missing required attribute [$PROPERTY_ATTR]")

		def bean = resolveBean(attrs.remove(BEAN_ATTR))
		String property = attrs.remove(PROPERTY_ATTR)
        String templatesFolder = attrs.remove(TEMPLATES_ATTR)
        String fieldFolder = attrs.remove(WRAPPER_ATTR)
        String widgetFolder = attrs.remove(WIDGET_ATTR)
		String theme = attrs.remove(THEME_ATTR)

		BeanPropertyAccessor propertyAccessor = resolveProperty(bean, property)
		if (propertyAccessor.domainProperty instanceof Embedded) {
			renderEmbeddedProperties(bean, propertyAccessor, attrs + [templates: templatesFolder, field: fieldFolder, input: widgetFolder, theme:theme])
		} else {
			Map model = buildModel(propertyAccessor, attrs)
			Map wrapperAttrs = [:]
			Map widgetAttrs = [:]

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
				model.widget = renderWidget(propertyAccessor, model, widgetAttrs, widgetFolder?:templatesFolder, theme)
			}


			String templateName = formFieldsTemplateService.getTemplateFor("wrapper")
			Map template = formFieldsTemplateService.findTemplate(propertyAccessor, templateName, fieldFolder?:templatesFolder, theme)
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
	  * @attr order A comma-separated list of properties to include in provided order
	  * @attr except A comma-separated list of properties to exclude
	  * @attr theme Theme name
      */
    def table = { attrs, body ->
        def collection = resolveBean(attrs.remove('collection'))
		PersistentEntity domainClass
        if (attrs.containsKey('domainClass')) {
            domainClass = grailsDomainClassMappingContext.getPersistentEntity((String)attrs.remove('domainClass'))
        } else {
            domainClass = (collection instanceof Collection) && collection ? resolveDomainClass(collection.iterator().next()) : null
        }
        if (domainClass) {
        def properties

        if (attrs.containsKey('properties')) {
            properties = attrs.remove('properties').collect {
				fieldsDomainPropertyFactory.build(domainClass.getPropertyByName(it))
			}
		} else {
			properties = resolvePersistentProperties(domainClass, attrs)
			if (attrs.containsKey('maxProperties')) {
				String maxProperties = attrs.remove('maxProperties')
				if(maxProperties.isInteger()){
					Integer maxPropertiesNumber = maxProperties as Integer
					if (maxPropertiesNumber.abs() < properties.size()) {
						properties = properties[0..(maxPropertiesNumber - 1)]
					}
				}
			}
		}

        String displayStyle = attrs.remove('displayStyle')
		String theme = attrs.remove(THEME_ATTR)

        out << render(template: "/templates/_fields/table",
                 model: [domainClass: domainClass, domainProperties: properties, collection: collection, displayStyle: displayStyle, theme:theme])
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
	 * @attr theme Theme name
	 */
	def widget = { attrs ->
		def bean = resolveBean(attrs.remove(BEAN_ATTR))
		if (!bean) throwTagError("Tag [input] is missing required attribute [$BEAN_ATTR]")
		if (!attrs.property) throwTagError("Tag [input] is missing required attribute [property]")

		attrs = getPrefixedAttributes(beanStack.innerAttributes) + attrs

		String property = attrs.remove(PROPERTY_ATTR)
        String widgetFolder = attrs.remove(WIDGET_ATTR)
		String theme = attrs.remove(THEME_ATTR)

		BeanPropertyAccessor propertyAccessor = resolveProperty(bean, property)
		Map model = buildModel(propertyAccessor, attrs)

		out << renderWidget(propertyAccessor, model, attrs, widgetFolder, theme)
	}

	/**
	 * @attr bean Name of the source bean in the GSP model.
	 * @attr property REQUIRED The name of the property to display. This is resolved
	 * against the specified bean or the bean in the current scope.
	 * @attr theme Theme name
	 */
	def displayWidget = { attrs ->
		def bean = resolveBean(attrs.remove(BEAN_ATTR))
		if (!bean) throwTagError("Tag [displayWidget] is missing required attribute [$BEAN_ATTR]")
		if (!attrs[PROPERTY_ATTR]) throwTagError("Tag [displayWidget] is missing required attribute [property]")

		attrs = getPrefixedAttributes(beanStack.innerAttributes) + attrs

		String property = attrs.remove(PROPERTY_ATTR)
        String widgetFolder = attrs.remove(WIDGET_ATTR)
		String theme = attrs.remove(THEME_ATTR)

		BeanPropertyAccessor propertyAccessor = resolveProperty(bean, property)
		Map model = buildModel(propertyAccessor, attrs)

		out << renderDisplayWidget(propertyAccessor, model, attrs, widgetFolder, theme)
	}

	/**
	 * @attr bean Name of the source bean in the GSP model.
	 * @attr property The name of the property to display. This is resolved against the specified bean or the bean in the current scope.
	 * @attr order A comma-separated list of properties to include in provided order
	 * @attr except A comma-separated list of properties to exclude
	 * @attr theme Theme name
	 */
	def display = { attrs, body ->
		attrs = beanStack.innerAttributes + attrs
		def bean = resolveBean(attrs.remove(BEAN_ATTR))
		if (!bean) throwTagError("Tag [display] is missing required attribute [$BEAN_ATTR]")

		String property = attrs.remove(PROPERTY_ATTR)
		String templatesFolder = attrs.remove(TEMPLATES_ATTR)
		String theme = attrs.remove(THEME_ATTR)

		if (property == null) {
			PersistentEntity domainClass = resolveDomainClass(bean)
			if (domainClass) {
				List properties = resolvePersistentProperties(domainClass, attrs)
				out << render(template: "/templates/_fields/list", model: [domainClass: domainClass, domainProperties: properties]) { prop ->
					BeanPropertyAccessor propertyAccessor = resolveProperty(bean, prop.name)
					Map model = buildModel(propertyAccessor, attrs)
					out << raw(renderDisplayWidget(propertyAccessor, model, attrs, templatesFolder, theme))
				}
			}
		} else {
            String displayFolder = attrs.remove(WRAPPER_ATTR)
            String widgetFolder = attrs.remove(WIDGET_ATTR)

			BeanPropertyAccessor propertyAccessor = resolveProperty(bean, property)
            Map model = buildModel(propertyAccessor, attrs)

            Map wrapperAttrs = [:]
            Map widgetAttrs = [:]

            String prefixAttribute = formFieldsTemplateService.getWidgetPrefix() ?: 'widget-'
            attrs.each { k, v ->
                if (k?.startsWith(prefixAttribute))
                    widgetAttrs[k.replace(prefixAttribute, '')] = v
                else
                    wrapperAttrs[k] = v
            }


			String wrapperFolderTouse = displayFolder ?: templatesFolder
			String widgetsFolderToUse = widgetFolder ?: templatesFolder

			if (hasBody(body)) {
                model.widget = raw(body(model + [attrs: widgetAttrs] + widgetAttrs))
                model.value = body(model)
            } else {
                model.widget = renderDisplayWidget(propertyAccessor, model, widgetAttrs, widgetsFolderToUse, theme)
            }


			def displayWrapperTemplateName = formFieldsTemplateService.getTemplateFor("displayWrapper")
			def template = formFieldsTemplateService.findTemplate(propertyAccessor, displayWrapperTemplateName, wrapperFolderTouse, theme)
            if (template) {
                out << render(template: template.path, plugin: template.plugin, model: model + [attrs: wrapperAttrs] + wrapperAttrs)
            } else {
				out << raw(renderDisplayWidget(propertyAccessor, model, attrs, widgetsFolderToUse, theme))
            }
        }

	}

	private Map getPrefixedAttributes(attrs) {
		Map widgetAttrs = [:]
		attrs.each { k, v ->
			String prefixAttribute = formFieldsTemplateService.getWidgetPrefix()
			if (k?.startsWith(prefixAttribute))
				widgetAttrs[k.replace(prefixAttribute, '')] = v
		}
		return widgetAttrs
	}

    private void renderEmbeddedProperties(bean, BeanPropertyAccessor propertyAccessor, attrs) {
		def legend = resolveMessage(propertyAccessor.labelKeys, propertyAccessor.defaultLabel)
		out << applyLayout(name: '_fields/embedded', params: [type: toPropertyNameFormat(propertyAccessor.propertyType), legend: legend]) {
			Embedded prop = (Embedded)propertyAccessor.domainProperty
			for (embeddedProp in resolvePersistentProperties(prop.associatedEntity, attrs)) {
				def propertyPath = "${propertyAccessor.pathFromRoot}.${embeddedProp.name}"
				out << field(bean: bean, property: propertyPath, prefix: attrs.prefix, default: attrs.default, field: attrs.field, widget: attrs.widget, theme:attrs[THEME_ATTR])
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
			beanClass: propertyAccessor.entity,
			label: resolveLabelText(propertyAccessor, attrs),
			value: (value instanceof Number || value instanceof Boolean || value) ? value : valueDefault,
			constraints: propertyAccessor.constraints,
			persistentProperty: propertyAccessor.domainProperty,
			errors: propertyAccessor.errors.collect { message(error: it) },
			required: attrs.containsKey("required") ? Boolean.valueOf(attrs.remove('required')) : propertyAccessor.required,
			invalid: attrs.containsKey("invalid") ? Boolean.valueOf(attrs.remove('invalid')) : propertyAccessor.invalid,
			prefix: resolvePrefix(attrs.remove('prefix')),
		]
	}

    private CharSequence renderWidget(BeanPropertyAccessor propertyAccessor, Map model, Map attrs = [:], String widgetFolder, String theme) {
		String widgetTemplateName = formFieldsTemplateService.getTemplateFor("widget")
		Map template = formFieldsTemplateService.findTemplate(propertyAccessor, widgetTemplateName, widgetFolder, theme)
		if (template) {
			render template: template.path, plugin: template.plugin, model: model + [attrs: attrs] + attrs
		} else {
			renderDefaultInput propertyAccessor, model, attrs
		}
	}

	private CharSequence renderDisplayWidget(BeanPropertyAccessor propertyAccessor, Map model, Map attrs = [:], String widgetFolder, String theme) {
		String displayStyle = attrs.displayStyle

		Map template = null
		if (displayStyle && displayStyle != 'default') {
			template = formFieldsTemplateService.findTemplate(propertyAccessor, "displayWidget-$attrs.displayStyle", widgetFolder, theme)
		}
		if (!template) {
			def displayWidgetTemplateName = formFieldsTemplateService.getTemplateFor("displayWidget")
			template = formFieldsTemplateService.findTemplate(propertyAccessor, displayWidgetTemplateName, widgetFolder, theme)
		}

		if (template) {
			render template: template.path, plugin: template.plugin, model: model + [attrs: attrs] + attrs
		} else if (!(model.value instanceof CharSequence)) {
			renderDefaultDisplay(model, attrs, widgetFolder, theme)
		} else {
			model.value
		}
	}

    private resolvePageScopeVariable(attributeName) {
        // Tomcat throws NPE if you query pageScope for null/empty values
        attributeName?.toString() ? pageScope.variables[attributeName] : null
    }

    private BeanAndPrefix resolveBeanAndPrefix(beanAttribute, prefixAttribute, attributes) {
        def bean = resolvePageScopeVariable(beanAttribute) ?: beanAttribute
        def prefix = resolvePageScopeVariable(prefixAttribute) ?: prefixAttribute
		def innerAttributes = attributes.clone()
		innerAttributes.remove('bean')
		innerAttributes.remove('prefix')
		//'except' is a reserved word for the 'all' tag: https://github.com/grails-fields-plugin/grails-fields/issues/12
		innerAttributes.remove('except')
		new BeanAndPrefix(bean: bean, prefix: prefix, innerAttributes: innerAttributes)
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

	private PersistentEntity resolveDomainClass(bean) {
		resolveDomainClass(bean.getClass())
	}

	private PersistentEntity resolveDomainClass(Class beanClass) {
		grailsDomainClassMappingContext.getPersistentEntity(beanClass.name)
	}

    private List<PersistentProperty> resolvePersistentProperties(PersistentEntity domainClass, Map attrs) {
        List<PersistentProperty> properties

        if(attrs.order) {
            if(attrs.except) {
                throwTagError('The [except] and [order] attributes may not be used together.')
            }
            def orderBy = attrs.order?.tokenize(',')*.trim() ?: []
            properties = orderBy.collect { propertyName ->
				fieldsDomainPropertyFactory.build(domainClass.getPropertyByName(propertyName))
			}
        } else {
			properties = domainModelService.getInputProperties(domainClass)
            def blacklist = attrs.except?.tokenize(',')*.trim() ?: []
			properties.removeAll { it.name in blacklist }
        }

		return properties
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
		return labelText
	}

	private CharSequence resolveMessage(List<String> keysInPreferenceOrder, String defaultMessage) {
		def message = keysInPreferenceOrder.findResult { key ->
			message(code: key, default: null) ?: null
		}
		message ?: defaultMessage
	}

	private CharSequence renderDefaultField(Map model) {
		List classes = ['fieldcontain']
		if (model.invalid) classes << 'error'
		if (model.required) classes << 'required'

		Writer writer = new FastStringWriter()
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

	CharSequence renderDefaultInput(Map model, Map attrs = [:]) {
		renderDefaultInput(null, model, attrs)
	}

	CharSequence renderDefaultInput(BeanPropertyAccessor propertyAccessor,Map model, Map attrs = [:]) {
		attrs.name = (model.prefix ?: '') + model.property
		attrs.value = model.value
		if (model.required) attrs.required = "" // TODO: configurable how this gets output? Some people prefer required="required"
		if (model.invalid) attrs.invalid = ""
		if (!isEditable(model.constraints)) attrs.readonly = ""

		boolean oneToOne = false
		boolean manyToOne = false
		boolean manyToMany = false
		boolean oneToMany = false
		if (model.containsKey('persistentProperty')) {
			if (model.persistentProperty instanceof GrailsDomainClassProperty) {
				log.warn("Rendering an input with a GrailsDomainClassProperty is deprecated. Use a PersistentProperty instead.")
				GrailsDomainClassProperty gdcp = (GrailsDomainClassProperty)model.persistentProperty
				oneToOne = gdcp.oneToOne
				manyToOne = gdcp.manyToOne
				manyToMany = gdcp.manyToMany
				oneToMany = gdcp.oneToMany
			} else if (model.persistentProperty instanceof PersistentProperty) {
				PersistentProperty prop = (PersistentProperty)model.persistentProperty
				oneToOne = prop instanceof OneToOne
				manyToOne = prop instanceof ManyToOne
				manyToMany = prop instanceof ManyToMany
				oneToMany = prop instanceof OneToMany
			}
		}

		if (model.type in [String, null]) {
			return renderStringInput(model, attrs)
		} else if (model.type in [boolean, Boolean]) {
			return g.checkBox(attrs)
		} else if (model.type.isPrimitive() || model.type in Number) {
			return renderNumericInput(propertyAccessor, model, attrs)
		} else if (model.type in URL) {
			return g.field(attrs + [type: "url"])
		} else if (model.type.isEnum()) {
			return renderEnumInput(model,attrs)
		} else if (oneToOne || manyToOne || manyToMany) {
			return renderAssociationInput(model, attrs)
		} else if (oneToMany) {
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


    CharSequence renderDateTimeInput(Map model, Map attrs) {
        attrs.precision = model.type == java.sql.Time ? "minute" : "day"
        if (!model.required) {
            attrs.noSelection = ["": ""]
            attrs.default = "none"
        }
        return g.datePicker(attrs)
    }

    CharSequence renderStringInput(Map model, Map attrs) {
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

    CharSequence renderNumericInput(BeanPropertyAccessor propertyAccessor,Map model, Map attrs) {
        if (!attrs.type && model.constraints?.inList) {
            attrs.from = model.constraints.inList
            if (!model.required) attrs.noSelection = ["": ""]
            return g.select(attrs)
        } else if (model.constraints?.range) {
            attrs.type = attrs.type ?: "range"
            attrs.min = model.constraints.range.from
            attrs.max = model.constraints.range.to
        } else {
            attrs.type = attrs.type ?: getDefaultNumberType(model )
            if (model.constraints?.scale != null) attrs.step = "0.${'0' * (model.constraints.scale - 1)}1"
            if (model.constraints?.min != null) attrs.min = model.constraints.min
            if (model.constraints?.max != null) attrs.max = model.constraints.max
        }

        return g.field(attrs)
    }

	@CompileStatic
	protected String getDefaultNumberType(Map model) {
		Class modelType = (Class)model.type

		def typeName = modelType.simpleName.toLowerCase()
		if(typeName in DECIMAL_TYPES) {
			return "number decimal"
		}
		else {
			return "number"
		}

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
        Writer buffer = new FastStringWriter()
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

    private CharSequence renderDefaultDisplay(Map model, Map attrs = [:], String templatesFolder, String theme) {
        def persistentProperty = model.persistentProperty
        if (persistentProperty instanceof Association) {
            if (persistentProperty.embedded) {
                return (attrs.displayStyle == 'table') ? model.value?.toString().encodeAsHTML() :
                        displayEmbedded(model.value, persistentProperty.associatedEntity, attrs, templatesFolder, theme)
            } else if (persistentProperty instanceof OneToMany || persistentProperty instanceof ManyToMany) {
                return displayAssociationList(model.value, ((Association)persistentProperty).associatedEntity)
            } else {
                return displayAssociation(model.value, ((Association)persistentProperty).associatedEntity)
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

    private CharSequence displayEmbedded(bean, PersistentEntity domainClass, Map attrs, String templatesFolder, String theme) {
        Writer buffer = new FastStringWriter()
		def properties = domainModelService.getOutputProperties(domainClass)
        buffer << render(template: "/templates/_fields/list", model: [domainClass: domainClass, domainProperties: properties]) { prop ->
            def propertyAccessor = resolveProperty(bean, prop.name)
            def model = buildModel(propertyAccessor, attrs)
            out << raw(renderDisplayWidget(propertyAccessor, model, attrs, templatesFolder, theme))
        }
        buffer.buffer
    }

    private CharSequence displayAssociationList(values, PersistentEntity referencedDomainClass) {
        Writer buffer = new FastStringWriter()
        buffer << '<ul>'
        for (value in values) {
            buffer << '<li>'
            buffer << displayAssociation(value, referencedDomainClass)
            buffer << '</li>'
        }
        buffer << '</ul>'
        buffer.buffer
    }

    private CharSequence displayAssociation(value, PersistentEntity referencedDomainClass) {
        if(value) {
            g.link(controller: referencedDomainClass.decapitalizedName, action: "show", id: value.id, value.toString().encodeAsHTML())
        }
    }

    private boolean isEditable(constraints) {
        !constraints || constraints.editable
    }
}
