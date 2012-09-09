package grails.plugin.formfields

import grails.plugin.formfields.mock.Address
import grails.plugin.formfields.mock.Employee
import grails.plugin.formfields.mock.Gender
import grails.plugin.formfields.mock.Person
import grails.plugin.formfields.mock.Salutation
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import spock.lang.Specification

@TestMixin(GroovyPageUnitTestMixin)
@TestFor(FormFieldsTemplateService)
class ConfigurableFormFieldsTemplateServiceSpec extends Specification {

    Person personInstance
    Employee employeeInstance
    def factory = new BeanPropertyAccessorFactory()

    void setup() {
        webRequest.controllerName = 'foo'
        webRequest.actionName = 'bar'

        personInstance = new Person(name: "Bart Simpson", password: "bartman", gender: Gender.Male, dateOfBirth: new Date(87, 3, 19), minor: true)
        personInstance.address = new Address(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")

        employeeInstance = new Employee(salutation: Salutation.MR, name: "Waylon Smithers", salary: 10)

        factory.grailsApplication = grailsApplication
        factory.constraintsEvaluator = new DefaultConstraintEvaluator()
        factory.proxyHandler = new DefaultProxyHandler()

        service.ignoreControllerLookup = false
        service.ignoreControllerActionLookup = false
        service.ignoreBeanTypeLookup = false
        service.ignoreBeanSuperTypeLookup = false
        service.ignorePropertyTypeLookup = false
        service.ignorePropertySuperTypeLookup = false
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

    void "ignores template for property type"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
        service.ignorePropertyTypeLookup = true

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == '/_fields/default/field'
        template.plugin == null
        render(template: template.path) == 'DEFAULT FIELD TEMPLATE'
    }

    void "ignores template for domain class property"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
        views["/_fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        service.ignoreBeanTypeLookup = true

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == '/_fields/string/field'
        template.plugin == null
        render(template: template.path) == 'PROPERTY TYPE TEMPLATE'
    }

    void "ignores template from controller views directory"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
        views["/_fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_field.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        service.ignoreControllerLookup = true

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == "/_fields/person/name/field"
        template.plugin == null
        render(template: template.path) == 'CLASS AND PROPERTY TEMPLATE'
    }

    void "ignores template by property type from controller views directory"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
        views["/_fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_field.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_field.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
        service.ignoreControllerLookup = true

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == "/_fields/person/name/field"
        template.plugin == null
        render(template: template.path) == 'CLASS AND PROPERTY TEMPLATE'
    }

    void "ignores template by property name from controller views directory"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
        views["/_fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_field.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_field.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
        views["/$webRequest.controllerName/name/_field.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'
        service.ignoreControllerLookup = true

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == "/_fields/person/name/field"
        template.plugin == null
        render(template: template.path) == 'CLASS AND PROPERTY TEMPLATE'
    }

    void "ignores template by controller and action views directory"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
        views["/_fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_field.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_field.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
        views["/$webRequest.controllerName/name/_field.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/_field.gsp"] = 'ACTION DEFAULT TEMPLATE'
        service.ignoreControllerActionLookup = true

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == "/$webRequest.controllerName/name/field"
        template.plugin == null
        render(template: template.path) == 'CONTROLLER FIELD NAME TEMPLATE'
    }

    void "ignores template by property type from controller and action views directory"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
        views["/_fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_field.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_field.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
        views["/$webRequest.controllerName/name/_field.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/_field.gsp"] = 'ACTION DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/string/_field.gsp"] = 'ACTION FIELD TYPE TEMPLATE'
        service.ignoreControllerActionLookup = true

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == "/$webRequest.controllerName/name/field"
        template.plugin == null
        render(template: template.path) == 'CONTROLLER FIELD NAME TEMPLATE'
    }

    void "ignores template by property name from controller and action views directory"() {
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
        service.ignoreControllerActionLookup = true

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == "/$webRequest.controllerName/name/field"
        template.plugin == null
        render(template: template.path) == 'CONTROLLER FIELD NAME TEMPLATE'
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

    def "ignores template for superclass property"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/person/name/_field.gsp"] = 'SUPERCLASS TEMPLATE'
        service.ignoreBeanSuperTypeLookup = true

        and:
        def property = factory.accessorFor(employeeInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == '/_fields/default/field'
        template.plugin == null
        render(template: template.path) == 'DEFAULT FIELD TEMPLATE'
    }

    def "subclass property template overrides superclass property template"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/person/name/_field.gsp"] = 'SUPERCLASS TEMPLATE'
        views["/_fields/employee/name/_field.gsp"] = 'SUBCLASS TEMPLATE'
        service.ignoreBeanSuperTypeLookup = true

        and:
        def property = factory.accessorFor(employeeInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == '/_fields/employee/name/field'
        template.plugin == null
        render(template: template.path) == 'SUBCLASS TEMPLATE'
    }

    def "property template gets ignore by the property's superclass"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/enum/_field.gsp"] = 'GENERIC ENUM TEMPLATE'
        service.ignorePropertySuperTypeLookup = true

        and:
        def property = factory.accessorFor(employeeInstance, 'salutation')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == '/_fields/default/field'
        template.plugin == null
        render(template: template.path) == 'DEFAULT FIELD TEMPLATE'
    }

    def "property template gets ignored by the property's interface"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/charSequence/_field.gsp"] = 'INTERFACE TEMPLATE'
        service.ignorePropertySuperTypeLookup = true

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == '/_fields/default/field'
        template.plugin == null
        render(template: template.path) == 'DEFAULT FIELD TEMPLATE'
    }

    def "property template overrides property's superclass template"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/enum/_field.gsp"] = 'ENUM TEMPLATE'
        views["/_fields/salutation/_field.gsp"] = 'SALUTATION TEMPLATE'
        service.ignorePropertySuperTypeLookup = true

        and:
        def property = factory.accessorFor(employeeInstance, 'salutation')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == '/_fields/salutation/field'
        template.plugin == null
        render(template: template.path) == 'SALUTATION TEMPLATE'
    }

    void "ignores template for embedded class property"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
        views["/_fields/address/city/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        service.ignoreBeanTypeLookup = true

        and:
        def property = factory.accessorFor(personInstance, 'address.city')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == '/_fields/string/field'
        template.plugin == null
        render(template: template.path) == 'PROPERTY TYPE TEMPLATE'
    }

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

    void 'ignores controller template without a bean just based on property path'() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/$webRequest.controllerName/name/_field.gsp"] = 'CONTROLLER FIELD TEMPLATE'
        service.ignoreControllerLookup = true

        and:
        def property = factory.accessorFor(null, 'name')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == '/_fields/default/field'
        template.plugin == null
        render(template: template.path) == 'DEFAULT FIELD TEMPLATE'
    }

    void 'ignores controller action template without a bean just based on property path'() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/$webRequest.controllerName/name/_field.gsp"] = 'CONTROLLER FIELD TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/name/_field.gsp"] = 'ACTION FIELD TEMPLATE'
        service.ignoreControllerActionLookup = true

        and:
        def property = factory.accessorFor(null, 'name')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == "/$webRequest.controllerName/name/field"
        template.plugin == null
        render(template: template.path) == 'CONTROLLER FIELD TEMPLATE'
    }

    void "uses default template by property type from controller and action views directory"() {
        given:
        views["/_fields/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/_fields/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
        views["/_fields/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
        views["/$webRequest.controllerName/_field.gsp"] = 'CONTROLLER DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/string/_field.gsp"] = 'CONTROLLER FIELD TYPE TEMPLATE'
        views["/$webRequest.controllerName/name/_field.gsp"] = 'CONTROLLER FIELD NAME TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/_field.gsp"] = 'ACTION DEFAULT TEMPLATE'
        views["/$webRequest.controllerName/$webRequest.actionName/string/_field.gsp"] = 'ACTION FIELD TYPE TEMPLATE'
        service.ignorePropertyTypeLookup = true
        service.ignoreBeanTypeLookup = true
        service.ignoreControllerLookup = true
        service.ignoreControllerActionLookup = true

        and:
        def property = factory.accessorFor(personInstance, 'name')

        expect:
        def template = service.findTemplate(property, 'field')
        template.path == "/_fields/default/field"
        template.plugin == null
        render(template: template.path) == 'DEFAULT FIELD TEMPLATE'
    }
}
