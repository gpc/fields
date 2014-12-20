package grails.plugin.formfields

import grails.plugin.formfields.mock.*
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import spock.lang.IgnoreIf
import spock.lang.Specification

@TestMixin(GroovyPageUnitTestMixin)
@TestFor(FormFieldsTemplateService)
class FormFieldsTemplateServiceLayoutSpec extends Specification {

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

	void 'use no layout when no others exist'() {
		given:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template == null
	}

	void 'uses default layout when no others exist'() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT LAYOUT TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == '/_fields/default/layout'
		template.plugin == null
		render(template: template.path) == 'DEFAULT LAYOUT TEMPLATE'
	}

	void "resolves layout for property type"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT LAYOUT TEMPLATE'
		views["/_fields/string/_layout.gsp"] = 'PROPERTY TYPE TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == '/_fields/string/layout'
		template.plugin == null
		render(template: template.path) == 'PROPERTY TYPE TEMPLATE'
	}

	void "resolves layout for widget constraint"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_layout.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/fancywidget/_layout.gsp"] = 'WIDGET TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == '/_fields/fancywidget/layout'
		template.plugin == null
		render(template: template.path) == 'WIDGET TEMPLATE'
	}

    void "resolves layout for component field"() {
        given:
        views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/string/_layout.gsp"] = 'PROPERTY TYPE TEMPLATE'
        views["/_fields/fancywidget/_layout.gsp"] = 'WIDGET TEMPLATE'
        views["/_fields/_components/time/_layout.gsp"] = 'COMPONENT TEMPLATE'

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findLayout(property, "time", null)
        template.path == '/_fields/_components/time/layout'
        template.plugin == null
        render(template: template.path) == 'COMPONENT TEMPLATE'
    }

	void "resolves template for implicit widget constraint"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_layout.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/password/_layout.gsp"] = 'WIDGET TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'password')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == '/_fields/password/layout'
		template.plugin == null
		render(template: template.path) == 'WIDGET TEMPLATE'
	}

	void "resolves layout for domain class property"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_layout.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_layout.gsp"] = 'CLASS AND PROPERTY TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == '/_fields/person/name/layout'
		template.plugin == null
		render(template: template.path) == 'CLASS AND PROPERTY TEMPLATE'
	}

    void "resolves layout from controller views directory"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_layout.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_layout.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
		views["/$webRequest.controllerName/_layout.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == "/$webRequest.controllerName/layout"
		template.plugin == null
		render(template: template.path) == 'CONTROLLER DEFAULT TEMPLATE'
	}

	@IgnoreIf({ GrailsWebRequest.metaClass.respondsTo(GrailsWebRequest, "getControllerNamespace").size() == 0 })
	void "resolves layout from namespaced controller views directory"() {
		given:
		webRequest.setControllerNamespace('namespace')
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_layout.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_layout.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
		views["/$webRequest.controllerNamespace/$webRequest.controllerName/_layout.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == "/$webRequest.controllerNamespace/$webRequest.controllerName/layout"
		template.plugin == null
		render(template: template.path) == 'CONTROLLER DEFAULT TEMPLATE'
	}

	void "resolves layout by property type from controller views directory"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_layout.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_layout.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
		views["/$webRequest.controllerName/_layout.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
		views["/$webRequest.controllerName/string/_layout.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == "/$webRequest.controllerName/string/layout"
		template.plugin == null
		render(template: template.path) == 'CONTROLLER FIELD TYPE TEMPLATE'
	}

	void "resolves layout by property name from controller views directory"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_layout.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_layout.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_layout.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_layout.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
		views["/$webRequest.controllerName/name/_layout.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == "/$webRequest.controllerName/name/layout"
		template.plugin == null
		render(template: template.path) == 'CONTROLLER FIELD NAME TEMPLATE'
	}

    void "resolves layout by from controller and action views directory"() {
        given:
        views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/string/_layout.gsp"] = 'PROPERTY TYPE TEMPLATE'
        views["/_fields/person/name/_layout.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_layout.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_layout.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
        views["/$webRequest.controllerName/name/_layout.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/_layout.gsp"] = 'ACTION DEFAULT TEMPLATE'

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findLayout(property, null, null)
        template.path == "/$webRequest.controllerName/$webRequest.actionName/layout"
        template.plugin == null
        render(template: template.path) == 'ACTION DEFAULT TEMPLATE'
    }

	void "resolves layout by property type from controller and action views directory"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_layout.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_layout.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_layout.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_layout.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
		views["/$webRequest.controllerName/name/_layout.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/_layout.gsp"] = 'ACTION DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/string/_layout.gsp"] = 'ACTION FIELD TYPE TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == "/$webRequest.controllerName/$webRequest.actionName/string/layout"
		template.plugin == null
		render(template: template.path) == 'ACTION FIELD TYPE TEMPLATE'
	}

	void "resolves layout by property name from controller and action views directory"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_layout.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_layout.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_layout.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_layout.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
		views["/$webRequest.controllerName/name/_layout.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/_layout.gsp"] = 'ACTION DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/string/_layout.gsp"] = 'ACTION FIELD TYPE TEMPLATE'
		views["/$webRequest.controllerName/$webRequest.actionName/name/_layout.gsp"] = 'ACTION FIELD NAME TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == "/$webRequest.controllerName/$webRequest.actionName/name/layout"
		template.plugin == null
		render(template: template.path) == 'ACTION FIELD NAME TEMPLATE'
	}

	void "does not use controller if there isn't one in the current request"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/name/_layout.gsp"] = 'STRANGE TEMPLATE'

		and:
		webRequest.controllerName = null

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == '/_fields/default/layout'
		template.plugin == null
		render(template: template.path) == 'DEFAULT FIELD TEMPLATE'
	}

	def "resolves layout for superclass property"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/person/name/_layout.gsp"] = 'SUPERCLASS TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == '/_fields/person/name/layout'
		template.plugin == null
		render(template: template.path) == 'SUPERCLASS TEMPLATE'
	}

	def "subclass property template overrides superclass property layout"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/person/name/_layout.gsp"] = 'SUPERCLASS TEMPLATE'
		views["/_fields/employee/name/_layout.gsp"] = 'SUBCLASS TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == '/_fields/employee/name/layout'
		template.plugin == null
		render(template: template.path) == 'SUBCLASS TEMPLATE'
	}

	def "property layout gets resolved by the property's superclass"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/enum/_layout.gsp"] = 'GENERIC ENUM TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'salutation')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == '/_fields/enum/layout'
		template.plugin == null
		render(template: template.path) == 'GENERIC ENUM TEMPLATE'
	}

	def "property layout gets resolved by the property's interface"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/charSequence/_layout.gsp"] = 'INTERFACE TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == '/_fields/charSequence/layout'
		template.plugin == null
		render(template: template.path) == 'INTERFACE TEMPLATE'
	}

	def "property layout overrides property's superclass template"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/enum/_layout.gsp"] = 'ENUM TEMPLATE'
		views["/_fields/salutation/_layout.gsp"] = 'SALUTATION TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'salutation')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == '/_fields/salutation/layout'
		template.plugin == null
		render(template: template.path) == 'SALUTATION TEMPLATE'
	}

	void "resolves layout for embedded class property"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_layout.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/address/city/_layout.gsp"] = 'CLASS AND PROPERTY TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'address.city')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == '/_fields/address/city/layout'
		template.plugin == null
		render(template: template.path) == 'CLASS AND PROPERTY TEMPLATE'
	}

	void 'resolves layout without a bean just based on property path'() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'

		and:
		def property = factory.accessorFor(null, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == '/_fields/default/layout'
		template.plugin == null
		render(template: template.path) == 'DEFAULT FIELD TEMPLATE'
	}

	void 'resolves controller layout without a bean just based on property path'() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/$webRequest.controllerName/name/_layout.gsp"] = 'CONTROLLER FIELD TEMPLATE'

		and:
		def property = factory.accessorFor(null, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == "/$webRequest.controllerName/name/layout"
		template.plugin == null
		render(template: template.path) == 'CONTROLLER FIELD TEMPLATE'
	}

	void 'resolves controller action layout without a bean just based on property path'() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/$webRequest.controllerName/name/_layout.gsp"] = 'CONTROLLER FIELD TEMPLATE'
		views["/$webRequest.controllerName/$webRequest.actionName/name/_layout.gsp"] = 'ACTION FIELD TEMPLATE'

		and:
		def property = factory.accessorFor(null, 'name')

		expect:
		def template = service.findLayout(property, null, null)
		template.path == "/$webRequest.controllerName/$webRequest.actionName/name/layout"
		template.plugin == null
		render(template: template.path) == 'ACTION FIELD TEMPLATE'
	}

    void "resolves layout for property type byte[]"() {
        given:
        views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/byteArray/_layout.gsp"] = 'PROPERTY BYTE ARRAY TYPE TEMPLATE'
        views["/_fields/byte;/_layout.gsp"] = 'PROPERTY WRONG BYTE ARRAY TYPE TEMPLATE'

        and:
        def property = factory.accessorFor(personInstance, 'picture')

        expect:
        def template = service.findLayout(property, null, null)
        template.path == '/_fields/byteArray/layout'
        template.plugin == null
        render(template: template.path) == 'PROPERTY BYTE ARRAY TYPE TEMPLATE'
    }

	void "layout attribute should be priority"() {
		given:
		views["/_fields/default/_layout.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_layout.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_layout.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
		views["/$webRequest.controllerName/_layout.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
		views["/$webRequest.controllerName/string/_layout.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
		views["/$webRequest.controllerName/name/_layout.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'
		views["/$webRequest.controllerName/$webRequest.actionName/_layout.gsp"] = 'ACTION DEFAULT TEMPLATE'
		views["/$webRequest.controllerName/$webRequest.actionName/string/_layout.gsp"] = 'ACTION FIELD TYPE TEMPLATE'
		views["/$webRequest.controllerName/$webRequest.actionName/name/_layout.gsp"] = 'ACTION FIELD NAME TEMPLATE'
		views["/_fields/_layouts/_someLayout.gsp"] = 'SOME LAYOUT'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findLayout(property, null, "someLayout")
		template.path == "/_fields/_layouts/someLayout"
		template.plugin == null
		render(template: template.path) == 'SOME LAYOUT'
	}
}
