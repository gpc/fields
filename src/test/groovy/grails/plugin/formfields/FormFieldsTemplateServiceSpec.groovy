package grails.plugin.formfields

import grails.core.support.proxy.DefaultProxyHandler
import grails.test.mixin.web.GroovyPageUnitTestMixin
import grails.plugin.formfields.mock.*
import grails.test.mixin.*
import org.grails.validation.DefaultConstraintEvaluator
import org.grails.web.servlet.mvc.GrailsWebRequest
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
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == '/_fields/default/wrapper'
		template.plugin == null
		render(template: template.path) == 'DEFAULT FIELD TEMPLATE'
	}

	void "resolves template for property type"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_wrapper.gsp"] = 'PROPERTY TYPE TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == '/_fields/string/wrapper'
		template.plugin == null
		render(template: template.path) == 'PROPERTY TYPE TEMPLATE'
	}

	void "resolves template for password field"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_wrapper.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/password/_wrapper.gsp"] = 'WIDGET TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'password')

		expect:
		def template = service.findTemplate(property, 'wrapper', "password")
		template.path == '/_fields/password/wrapper'
		template.plugin == null
		render(template: template.path) == 'WIDGET TEMPLATE'
	}

    void "resolves template for password widget"() {
		given:
		views["/_fields/default/_widget.gsp"] = 'DEFAULT FIELD WIDGET'
		views["/_fields/string/_widget.gsp"] = 'PROPERTY TYPE WIDGET'
		views["/_fields/password/_widget.gsp"] = 'INPUT WIDGET'

		and:
		def property = factory.accessorFor(personInstance, 'password')

		expect:
		def template = service.findTemplate(property, 'widget', "password")
		template.path == '/_fields/password/widget'
		template.plugin == null
		render(template: template.path) == 'INPUT WIDGET'
	}

    void "resolves template for password display"() {
		given:
		views["/_fields/default/_displayWrapper.gsp"] = 'DEFAULT DISPLAY TEMPLATE'
		views["/_fields/string/_displayWrapper.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/password/_displayWrapper.gsp"] = 'OUTPUT TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'password')

		expect:
		def template = service.findTemplate(property, 'displayWrapper', "password")
		template.path == '/_fields/password/displayWrapper'
		template.plugin == null
		render(template: template.path) == 'OUTPUT TEMPLATE'
	}

    void "resolves template for display password widget"() {
		given:
		views["/_fields/default/_displayWidget.gsp"] = 'DEFAULT FIELD FOR DISPLAY'
		views["/_fields/string/_displayWidget.gsp"] = 'PROPERTY TYPE FOR DISPLAY'
		views["/_fields/password/_displayWidget.gsp"] = 'OUTPUT FOR DISPLAY'

		and:
		def property = factory.accessorFor(personInstance, 'password')

		expect:
		def template = service.findTemplate(property, 'displayWidget', "password")
		template.path == '/_fields/password/displayWidget'
		template.plugin == null
		render(template: template.path) == 'OUTPUT FOR DISPLAY'
	}

	void "resolves wrapper template for textarea widget constraint"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_wrapper.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/textarea/_wrapper.gsp"] = 'WIDGET TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'biography')

		expect:
		def template = service.findTemplate(property, 'wrapper', "biography")
		template.path == '/_fields/textarea/wrapper'
		template.plugin == null
		render(template: template.path) == 'WIDGET TEMPLATE'
	}

    void "resolves widget template for textarea widget constraint"() {
		given:
		views["/_fields/default/_widget.gsp"] = 'DEFAULT FIELD WIDGET'
		views["/_fields/string/_widget.gsp"] = 'PROPERTY TYPE WIDGET'
		views["/_fields/textarea/_widget.gsp"] = 'INPUT WIDGET'

		and:
		def property = factory.accessorFor(personInstance, 'biography')

		expect:
		def template = service.findTemplate(property, 'widget', "biography")
		template.path == '/_fields/textarea/widget'
		template.plugin == null
		render(template: template.path) == 'INPUT WIDGET'
	}

    void "resolves display wrapper template for textarea widget constraint"() {
		given:
		views["/_fields/default/_displayWrapper.gsp"] = 'DEFAULT DISPLAY TEMPLATE'
		views["/_fields/string/_displayWrapper.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/textarea/_displayWrapper.gsp"] = 'OUTPUT TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'biography')

		expect:
		def template = service.findTemplate(property, 'displayWrapper', "biography")
		template.path == '/_fields/textarea/displayWrapper'
		template.plugin == null
		render(template: template.path) == 'OUTPUT TEMPLATE'
	}

    void "resolves display widget template for textarea widget constraint"() {
		given:
		views["/_fields/default/_displayWidget.gsp"] = 'DEFAULT FIELD FOR DISPLAY'
		views["/_fields/string/_displayWidget.gsp"] = 'PROPERTY TYPE FOR DISPLAY'
		views["/_fields/textarea/_displayWidget.gsp"] = 'OUTPUT FOR DISPLAY'

		and:
		def property = factory.accessorFor(personInstance, 'biography')

		expect:
		def template = service.findTemplate(property, 'displayWidget', "biography")
		template.path == '/_fields/textarea/displayWidget'
		template.plugin == null
		render(template: template.path) == 'OUTPUT FOR DISPLAY'
	}

	void "resolves template for domain class property"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_wrapper.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_wrapper.gsp"] = 'CLASS AND PROPERTY TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == '/_fields/person/name/wrapper'
		template.plugin == null
		render(template: template.path) == 'CLASS AND PROPERTY TEMPLATE'
	}

    @Issue('https://github.com/grails-fields-plugin/grails-fields/issues/88')
    void "resolves template from controller views directory"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_wrapper.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_wrapper.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
		views["/$webRequest.controllerName/_wrapper.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == "/$webRequest.controllerName/wrapper"
		template.plugin == null
		render(template: template.path) == 'CONTROLLER DEFAULT TEMPLATE'
	}

//	@IgnoreIf({ GrailsApplicationAttributes.metaClass.hasProperty(GrailsApplicationAttributes, "CONTROLLER_NAMESPACE_ATTRIBUTE") == false })
	@IgnoreIf({ GrailsWebRequest.metaClass.respondsTo(GrailsWebRequest, "getControllerNamespace").size() == 0 })
	@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/168')
	void "resolves template from namespaced controller views directory"() {
		given:
		webRequest.setControllerNamespace('namespace')
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_wrapper.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_wrapper.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
		views["/$webRequest.controllerNamespace/$webRequest.controllerName/_wrapper.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == "/$webRequest.controllerNamespace/$webRequest.controllerName/wrapper"
		template.plugin == null
		render(template: template.path) == 'CONTROLLER DEFAULT TEMPLATE'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/39')
	void "resolves template by property type from controller views directory"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_wrapper.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_wrapper.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
		views["/$webRequest.controllerName/_wrapper.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
		views["/$webRequest.controllerName/string/_wrapper.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == "/$webRequest.controllerName/string/wrapper"
		template.plugin == null
		render(template: template.path) == 'CONTROLLER FIELD TYPE TEMPLATE'
	}

	void "resolves template by property name from controller views directory"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_wrapper.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_wrapper.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_wrapper.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_wrapper.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
		views["/$webRequest.controllerName/name/_wrapper.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == "/$webRequest.controllerName/name/wrapper"
		template.plugin == null
		render(template: template.path) == 'CONTROLLER FIELD NAME TEMPLATE'
	}

    @Issue('https://github.com/grails-fields-plugin/grails-fields/issues/88')
    void "resolves template by from controller and action views directory"() {
        given:
        views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/string/_wrapper.gsp"] = 'PROPERTY TYPE TEMPLATE'
        views["/_fields/person/name/_wrapper.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_wrapper.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_wrapper.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
        views["/$webRequest.controllerName/name/_wrapper.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/_wrapper.gsp"] = 'ACTION DEFAULT TEMPLATE'

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'wrapper', null)
        template.path == "/$webRequest.controllerName/$webRequest.actionName/wrapper"
        template.plugin == null
        render(template: template.path) == 'ACTION DEFAULT TEMPLATE'
    }

    @Issue('https://github.com/grails-fields-plugin/grails-fields/issues/39')
	void "resolves template by property type from controller and action views directory"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_wrapper.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_wrapper.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_wrapper.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_wrapper.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
		views["/$webRequest.controllerName/name/_wrapper.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/_wrapper.gsp"] = 'ACTION DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/string/_wrapper.gsp"] = 'ACTION FIELD TYPE TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == "/$webRequest.controllerName/$webRequest.actionName/string/wrapper"
		template.plugin == null
		render(template: template.path) == 'ACTION FIELD TYPE TEMPLATE'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/33')
	void "resolves template by property name from controller and action views directory"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_wrapper.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/person/name/_wrapper.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_wrapper.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_wrapper.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
		views["/$webRequest.controllerName/name/_wrapper.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/_wrapper.gsp"] = 'ACTION DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/string/_wrapper.gsp"] = 'ACTION FIELD TYPE TEMPLATE'
		views["/$webRequest.controllerName/$webRequest.actionName/name/_wrapper.gsp"] = 'ACTION FIELD NAME TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == "/$webRequest.controllerName/$webRequest.actionName/name/wrapper"
		template.plugin == null
		render(template: template.path) == 'ACTION FIELD NAME TEMPLATE'
	}

	void "does not use controller if there isn't one in the current request"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/name/_wrapper.gsp"] = 'STRANGE TEMPLATE'

		and:
		webRequest.controllerName = null

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == '/_fields/default/wrapper'
		template.plugin == null
		render(template: template.path) == 'DEFAULT FIELD TEMPLATE'
	}

	def "resolves template for superclass property"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/person/name/_wrapper.gsp"] = 'SUPERCLASS TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == '/_fields/person/name/wrapper'
		template.plugin == null
		render(template: template.path) == 'SUPERCLASS TEMPLATE'
	}

	def "subclass property template overrides superclass property template"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/person/name/_wrapper.gsp"] = 'SUPERCLASS TEMPLATE'
		views["/_fields/employee/name/_wrapper.gsp"] = 'SUBCLASS TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == '/_fields/employee/name/wrapper'
		template.plugin == null
		render(template: template.path) == 'SUBCLASS TEMPLATE'
	}

	def "property template gets resolved by the property's superclass"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/enum/_wrapper.gsp"] = 'GENERIC ENUM TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'salutation')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == '/_fields/enum/wrapper'
		template.plugin == null
		render(template: template.path) == 'GENERIC ENUM TEMPLATE'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/19')
	def "property template gets resolved by the property's interface"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/charSequence/_wrapper.gsp"] = 'INTERFACE TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == '/_fields/charSequence/wrapper'
		template.plugin == null
		render(template: template.path) == 'INTERFACE TEMPLATE'
	}

	def "property template overrides property's superclass template"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/enum/_wrapper.gsp"] = 'ENUM TEMPLATE'
		views["/_fields/salutation/_wrapper.gsp"] = 'SALUTATION TEMPLATE'

		and:
		def property = factory.accessorFor(employeeInstance, 'salutation')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == '/_fields/salutation/wrapper'
		template.plugin == null
		render(template: template.path) == 'SALUTATION TEMPLATE'
	}

	void "resolves template for embedded class property"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/_fields/string/_wrapper.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/_fields/address/city/_wrapper.gsp"] = 'CLASS AND PROPERTY TEMPLATE'

		and:
		def property = factory.accessorFor(personInstance, 'address.city')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == '/_fields/address/city/wrapper'
		template.plugin == null
		render(template: template.path) == 'CLASS AND PROPERTY TEMPLATE'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/pull/16')
	void 'resolves template without a bean just based on property path'() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'

		and:
		def property = factory.accessorFor(null, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == '/_fields/default/wrapper'
		template.plugin == null
		render(template: template.path) == 'DEFAULT FIELD TEMPLATE'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/pull/16')
	void 'resolves controller template without a bean just based on property path'() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/$webRequest.controllerName/name/_wrapper.gsp"] = 'CONTROLLER FIELD TEMPLATE'

		and:
		def property = factory.accessorFor(null, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == "/$webRequest.controllerName/name/wrapper"
		template.plugin == null
		render(template: template.path) == 'CONTROLLER FIELD TEMPLATE'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/pull/33')
	void 'resolves controller action template without a bean just based on property path'() {
		given:
		views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/$webRequest.controllerName/name/_wrapper.gsp"] = 'CONTROLLER FIELD TEMPLATE'
		views["/$webRequest.controllerName/$webRequest.actionName/name/_wrapper.gsp"] = 'ACTION FIELD TEMPLATE'

		and:
		def property = factory.accessorFor(null, 'name')

		expect:
		def template = service.findTemplate(property, 'wrapper', null)
		template.path == "/$webRequest.controllerName/$webRequest.actionName/name/wrapper"
		template.plugin == null
		render(template: template.path) == 'ACTION FIELD TEMPLATE'
	}

    @Issue('https://github.com/gpc/grails-fields/issues/144')
    void "resolves template for property type object Byte array"() {
        given:
        views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/byteArray/_wrapper.gsp"] = 'PROPERTY BYTE ARRAY TYPE TEMPLATE'
        views["/_fields/byte;/_wrapper.gsp"] = 'PROPERTY WRONG BYTE ARRAY TYPE TEMPLATE'

        and:
        def property = factory.accessorFor(personInstance, 'picture')

        expect:
        def template = service.findTemplate(property, 'wrapper', null)
        template.path == '/_fields/byteArray/wrapper'
        template.plugin == null
        render(template: template.path) == 'PROPERTY BYTE ARRAY TYPE TEMPLATE'
    }

    @Issue('https://github.com/grails-fields-plugin/grails-fields/pull/164')
    void "does not fail if constrained property is null"() {
        expect:
        null == service.getWidget(null)
    }

    @Issue('https://github.com/gpc/grails-fields/issues/183')
    void "resolves template for property type simple type byte array"() {
        given:
        views["/_fields/default/_wrapper.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/byteArray/_wrapper.gsp"] = 'PROPERTY SIMPLE BYTE ARRAY TYPE TEMPLATE'
        views["/_fields/byte;/_wrapper.gsp"] = 'PROPERTY WRONG BYTE ARRAY TYPE TEMPLATE'
        views["/_fields/[B/_wrapper.gsp"] = 'PROPERTY WRONG BYTE ARRAY TYPE TEMPLATE'

        and:
        def property = factory.accessorFor(personInstance, 'anotherPicture')

        expect:
        def template = service.findTemplate(property, 'wrapper', null)
        template.path == '/_fields/byteArray/wrapper'
        template.plugin == null
        render(template: template.path) == 'PROPERTY SIMPLE BYTE ARRAY TYPE TEMPLATE'
    }


}
