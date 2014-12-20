package grails.plugin.formfields

import grails.test.mixin.web.GroovyPageUnitTestMixin
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import grails.plugin.formfields.mock.*
import grails.test.mixin.*
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import spock.lang.*

@TestMixin(GroovyPageUnitTestMixin)
@TestFor(FormFieldsTemplateService)
class FormFieldsTemplateServiceSpec extends Specification {

	Person personInstance
	Employee employeeInstance
	def factory = new BeanPropertyAccessorFactory()
	
	void setup() {

		webRequest.controllerName = 'foo'
		webRequest.actionName = 'bar'

		personInstance = new Person(name: "Bart Simpson", password: "bartman", gender: Gender.Male, dateOfBirth: new Date(87, 3, 19), minor: true, picture: "good looking".bytes)
		personInstance.address = new Address(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")

		employeeInstance = new Employee(salutation: Salutation.MR, name: "Waylon Smithers", salary: 10)

		factory.grailsApplication = grailsApplication
		factory.constraintsEvaluator = new DefaultConstraintEvaluator()
		factory.proxyHandler = new DefaultProxyHandler()
	}

	void cleanup() {
		views.clear()
		applicationContext.getBean("groovyPagesTemplateEngine").clearPageCache()
		applicationContext.getBean("groovyPagesTemplateRenderer").clearCache()
	}

	void 'uses default template when no others exist'() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/_fields/default/field'
		template.plugin == null
		render(template: template.path) == 'DEFAULT FIELD TEMPLATE'
	}

	void "resolves template for property type"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/_fields/string/field'
		template.plugin == null
		render(template: template.path) == 'PROPERTY TYPE TEMPLATE'
	}

	void "resolves template for widget constraint"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/fancywidget/_field.gsp"] = 'WIDGET TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/_fields/fancywidget/field'
		template.plugin == null
		render(template: template.path) == 'WIDGET TEMPLATE'
	}

    void "resolves template for component field"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
        views["/_fields/fancywidget/_field.gsp"] = 'WIDGET TEMPLATE'
        views["/_fields/_components/time/_field.gsp"] = 'COMPONENT TEMPLATE'

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'field', "time")
        template.path == '/_fields/_components/time/field'
        template.plugin == null
        render(template: template.path) == 'COMPONENT TEMPLATE'
    }

    void "resolves template for component display"() {
        given:
        views["/_fields/default/_display.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/string/_display.gsp"] = 'PROPERTY TYPE TEMPLATE'
        views["/_fields/fancywidget/_display.gsp"] = 'WIDGET TEMPLATE'
        views["/_fields/_components/time/_field.gsp"] = 'FIELD COMPONENT TEMPLATE'
        views["/_fields/_components/time/_display.gsp"] = 'DISPLAY COMPONENT TEMPLATE'

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'display', "time")
        template.path == '/_fields/_components/time/display'
        template.plugin == null
        render(template: template.path) == 'DISPLAY COMPONENT TEMPLATE'
    }

	void "resolves template for implicit widget constraint"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/password/_field.gsp"] = 'WIDGET TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'password')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/_fields/password/field'
		template.plugin == null
		render(template: template.path) == 'WIDGET TEMPLATE'
	}

	void "resolves template for domain class property"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/_fields/person/name/field'
		template.plugin == null
		render(template: template.path) == 'CLASS AND PROPERTY TEMPLATE'
	}

    @Issue('https://github.com/grails-fields-plugin/grails-fields/issues/88')
    void "resolves template from controller views directory"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
		views["/$webRequest.controllerName/_field.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == "/$webRequest.controllerName/field"
		template.plugin == null
		render(template: template.path) == 'CONTROLLER DEFAULT TEMPLATE'
	}

//	@IgnoreIf({ GrailsApplicationAttributes.metaClass.hasProperty(GrailsApplicationAttributes, "CONTROLLER_NAMESPACE_ATTRIBUTE") == false })
	@IgnoreIf({ GrailsWebRequest.metaClass.respondsTo(GrailsWebRequest, "getControllerNamespace").size() == 0 })
	@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/168')
	void "resolves template from namespaced controller views directory"() {
		given:
		webRequest.setControllerNamespace('namespace')
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
		views["/$webRequest.controllerNamespace/$webRequest.controllerName/_field.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == "/$webRequest.controllerNamespace/$webRequest.controllerName/field"
		template.plugin == null
		render(template: template.path) == 'CONTROLLER DEFAULT TEMPLATE'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/39')
	void "resolves template by property type from controller views directory"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
		views["/$webRequest.controllerName/_field.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
		views["/$webRequest.controllerName/string/_field.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == "/$webRequest.controllerName/string/field"
		template.plugin == null
		render(template: template.path) == 'CONTROLLER FIELD TYPE TEMPLATE'
	}

	void "resolves template by property name from controller views directory"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_field.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_field.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
		views["/$webRequest.controllerName/name/_field.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == "/$webRequest.controllerName/name/field"
		template.plugin == null
		render(template: template.path) == 'CONTROLLER FIELD NAME TEMPLATE'
	}

    @Issue('https://github.com/grails-fields-plugin/grails-fields/issues/88')
    void "resolves template by from controller and action views directory"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
        views["/_fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_field.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_field.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
        views["/$webRequest.controllerName/name/_field.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/_field.gsp"] = 'ACTION DEFAULT TEMPLATE'

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == "/$webRequest.controllerName/$webRequest.actionName/field"
        template.plugin == null
        render(template: template.path) == 'ACTION DEFAULT TEMPLATE'
    }

    @Issue('https://github.com/grails-fields-plugin/grails-fields/issues/39')
	void "resolves template by property type from controller and action views directory"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_field.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_field.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
		views["/$webRequest.controllerName/name/_field.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/_field.gsp"] = 'ACTION DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/string/_field.gsp"] = 'ACTION FIELD TYPE TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == "/$webRequest.controllerName/$webRequest.actionName/string/field"
		template.plugin == null
		render(template: template.path) == 'ACTION FIELD TYPE TEMPLATE'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/33')
	void "resolves template by property name from controller and action views directory"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_field.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_field.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
		views["/$webRequest.controllerName/name/_field.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/_field.gsp"] = 'ACTION DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/string/_field.gsp"] = 'ACTION FIELD TYPE TEMPLATE'
		views["/$webRequest.controllerName/$webRequest.actionName/name/_field.gsp"] = 'ACTION FIELD NAME TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == "/$webRequest.controllerName/$webRequest.actionName/name/field"
		template.plugin == null
		render(template: template.path) == 'ACTION FIELD NAME TEMPLATE'
	}

	void "does not use controller if there isn't one in the current request"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/name/_field.gsp"] = 'STRANGE TEMPLATE'

		and:
		webRequest.controllerName = null

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/_fields/default/field'
		template.plugin == null
		render(template: template.path) == 'DEFAULT FIELD TEMPLATE'
	}

	def "resolves template for superclass property"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/person/name/_field.gsp"] = 'SUPERCLASS TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/_fields/person/name/field'
		template.plugin == null
		render(template: template.path) == 'SUPERCLASS TEMPLATE'
	}

	def "subclass property template overrides superclass property template"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/person/name/_field.gsp"] = 'SUPERCLASS TEMPLATE'
		views["/_fields/employee/name/_field.gsp"] = 'SUBCLASS TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/_fields/employee/name/field'
		template.plugin == null
		render(template: template.path) == 'SUBCLASS TEMPLATE'
	}

	def "property template gets resolved by the property's superclass"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/enum/_field.gsp"] = 'GENERIC ENUM TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'salutation')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/_fields/enum/field'
		template.plugin == null
		render(template: template.path) == 'GENERIC ENUM TEMPLATE'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/19')
	def "property template gets resolved by the property's interface"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/charSequence/_field.gsp"] = 'INTERFACE TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/_fields/charSequence/field'
		template.plugin == null
		render(template: template.path) == 'INTERFACE TEMPLATE'
	}

	def "property template overrides property's superclass template"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/enum/_field.gsp"] = 'ENUM TEMPLATE'
		views["/_fields/salutation/_field.gsp"] = 'SALUTATION TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'salutation')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/_fields/salutation/field'
		template.plugin == null
		render(template: template.path) == 'SALUTATION TEMPLATE'
	}

	void "resolves template for embedded class property"() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/address/city/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'address.city')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/_fields/address/city/field'
		template.plugin == null
		render(template: template.path) == 'CLASS AND PROPERTY TEMPLATE'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/pull/16')
	void 'resolves template without a bean just based on property path'() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'

		and:
		def property = factory.accessorFor(null, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == '/_fields/default/field'
		template.plugin == null
		render(template: template.path) == 'DEFAULT FIELD TEMPLATE'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/pull/16')
	void 'resolves controller template without a bean just based on property path'() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/$webRequest.controllerName/name/_field.gsp"] = 'CONTROLLER FIELD TEMPLATE'

		and:
		def property = factory.accessorFor(null, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == "/$webRequest.controllerName/name/field"
		template.plugin == null
		render(template: template.path) == 'CONTROLLER FIELD TEMPLATE'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/pull/33')
	void 'resolves controller action template without a bean just based on property path'() {
		given:
		views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/$webRequest.controllerName/name/_field.gsp"] = 'CONTROLLER FIELD TEMPLATE'
		views["/$webRequest.controllerName/$webRequest.actionName/name/_field.gsp"] = 'ACTION FIELD TEMPLATE'

		and:
		def property = factory.accessorFor(null, 'name')

		expect:
		def template = service.findTemplate(property, 'field')
		template.path == "/$webRequest.controllerName/$webRequest.actionName/name/field"
		template.plugin == null
		render(template: template.path) == 'ACTION FIELD TEMPLATE'
	}

    @Issue('https://github.com/gpc/grails-fields/issues/144')
    void "resolves template for property type byte[]"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/byteArray/_field.gsp"] = 'PROPERTY BYTE ARRAY TYPE TEMPLATE'
        views["/_fields/byte;/_field.gsp"] = 'PROPERTY WRONG BYTE ARRAY TYPE TEMPLATE'

        and:
        def property = factory.accessorFor(personInstance, 'picture')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == '/_fields/byteArray/field'
        template.plugin == null
        render(template: template.path) == 'PROPERTY BYTE ARRAY TYPE TEMPLATE'
    }

    @Issue('https://github.com/grails-fields-plugin/grails-fields/pull/164')
    void "does not fail if constrained property is null"() {
        expect:
        null == service.getWidget(null)
    }

}
