package org.codehaus.groovy.grails.plugins.beanfields.taglib

import grails.persistence.Entity
import org.codehaus.groovy.grails.plugins.beanfields.BeanPropertyAccessorFactory
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import spock.lang.Specification
import grails.test.mixin.*
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator

@TestFor(FormFieldsTagLib)
@Mock([Person, Employee])
class FormFieldsTagLibSpec extends Specification {

	def personInstance

	def setupSpec() {
		defineBeans {
			constraintsEvaluator(DefaultConstraintEvaluator)
			beanPropertyAccessorFactory(BeanPropertyAccessorFactory) {
				constraintsEvaluator = ref('constraintsEvaluator')
			}
		}
	}

	def setup() {
		webRequest.controllerName = "person"

		personInstance = new Person(name: "Bart Simpson", password: "bartman", gender: "Male", dateOfBirth: new Date(87, 3, 19), minor: true)
		personInstance.address = new Address(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")
	}

	def cleanup() {
		views.clear()
		applicationContext.getBean("groovyPagesTemplateEngine").clearPageCache()
		applicationContext.getBean("groovyPagesTemplateRenderer").clearCache()

		messageSource.@messages.clear() // bit of a hack but messages don't get torn down otherwise
	}

	void "bean attribute is required"() {
		when:
		applyTemplate('<form:field property="name"/>')

		then:
		thrown GrailsTagException
	}

	void "property attribute is required"() {
		when:
		applyTemplate('<form:field bean="${personInstance}"/>', [personInstance: personInstance])

		then:
		thrown GrailsTagException
	}

	void "bean attribute must not be null"() {
		when:
		applyTemplate('<form:field bean="${personInstance}" property="name"/>', [personInstance: null])

		then:
		thrown GrailsTagException
	}

	void "bean attribute can be a String"() {
		given:
		views["/forms/default/_field.gsp"] = '${bean.getClass().simpleName}'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "Person"
	}

	void "bean attribute string must refer to variable in page"() {
		when:
		applyTemplate('<form:field bean="personInstance" property="name"/>')

		then:
		thrown GrailsTagException
	}

	void testUsesDefaultTemplate() {
		given:
		views["/forms/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'DEFAULT FIELD TEMPLATE'
	}

	void "resolves template for property type"() {
		given:
		views["/forms/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/forms/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'PROPERTY TYPE TEMPLATE'
	}

	void "resolves template for domain class property"() {
		given:
		views["/forms/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/forms/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/forms/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "CLASS AND PROPERTY TEMPLATE"
	}

	void "resolves template from controller views directory"() {
		given:
		views["/forms/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/forms/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/forms/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
		views["/person/name/_field.gsp"] = 'CONTROLLER FIELD TEMPLATE'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'CONTROLLER FIELD TEMPLATE'
	}

	def "resolves template for superclass property"() {
		given:
		views["/forms/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/forms/person/name/_field.gsp"] = 'SUPERCLASS TEMPLATE'

		and:
		def employeeInstance = new Employee(name: "Waylon Smithers", salary: 10)

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: employeeInstance]) == 'SUPERCLASS TEMPLATE'
	}

	def "subclass property template overrides superclass property template"() {
		given:
		views["/forms/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/forms/person/name/_field.gsp"] = 'SUPERCLASS TEMPLATE'
		views["/forms/employee/name/_field.gsp"] = 'SUBCLASS TEMPLATE'

		and:
		def employeeInstance = new Employee(name: "Waylon Smithers", salary: 10)

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: employeeInstance]) == 'SUBCLASS TEMPLATE'
	}

    def "property template gets resolved by the property's superclass"() {
        given:
        views["/forms/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/forms/enum/_field.gsp"] = 'ENUM TEMPLATE'

        and:
        def employeeInstance = new Employee(salutation: SalutationEnum.MR, name: "Waylon Smithers", salary: 10)

        expect:
        applyTemplate('<form:field bean="personInstance" property="salutation"/>', [personInstance: employeeInstance]) == 'ENUM TEMPLATE'
    }

    def "property template overrides property's superclass template"() {
        given:
        views["/forms/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
        views["/forms/enum/_field.gsp"] = 'ENUM TEMPLATE'
        views["/forms/salutationEnum/_field.gsp"] = 'SALUTATION TEMPLATE'

        and:
        def employeeInstance = new Employee(salutation: SalutationEnum.MR, name: "Waylon Smithers", salary: 10)

        expect:
        applyTemplate('<form:field bean="personInstance" property="salutation"/>', [personInstance: employeeInstance]) == 'SALUTATION TEMPLATE'
    }

    void "bean and property attributes are passed to template"() {
		given:
		views["/forms/default/_field.gsp"] = '${bean.getClass().simpleName}.${property}'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "Person.name"
	}

	void "constraints are passed to template"() {
		given:
		views["/forms/default/_field.gsp"] = 'nullable=${constraints.nullable}, blank=${constraints.blank}'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "nullable=false, blank=false"
	}

	void "label is resolved by convention and passed to template"() {
		given:
		views["/forms/default/_field.gsp"] = '<label>${label}</label>'

		and:
		messageSource.addMessage("Person.name.label", request.locale, "Name of person")

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
	}

	void "label is defaulted to natural property name"() {
		given:
		views["/forms/default/_field.gsp"] = '<label>${label}</label>'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<label>Name</label>"
		applyTemplate('<form:field bean="personInstance" property="dateOfBirth"/>', [personInstance: personInstance]) == "<label>Date Of Birth</label>"
	}

	void "label can be overridden by label attribute"() {
		given:
		views["/forms/default/_field.gsp"] = '<label>${label}</label>'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name" label="Name of person"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
	}

	void "label can be overridden by label key attribute"() {
		given:
		views["/forms/default/_field.gsp"] = '<label>${label}</label>'

		and:
		messageSource.addMessage("custom.name.label", request.locale, "Name of person")

		expect:
		applyTemplate('<form:field bean="personInstance" property="name" labelKey="custom.name.label"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
	}

	void "value is defaulted to property value"() {
		given:
		views["/forms/default/_field.gsp"] = '<g:formatDate date="${value}" format="yyyy-MM-dd"/>'

		expect:
		applyTemplate('<form:field bean="personInstance" property="dateOfBirth"/>', [personInstance: personInstance]) == "1987-04-19"
	}

	void "value is overridden by value attribute"() {
		given:
		views["/forms/default/_field.gsp"] = '${value}'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name" value="Bartholomew J. Simpson"/>', [personInstance: personInstance]) == "Bartholomew J. Simpson"
	}

	void "value falls back to default"() {
		given:
		views["/forms/default/_field.gsp"] = '${value}'

		and:
		personInstance.name = null

		expect:
		applyTemplate('<form:field bean="personInstance" property="name" default="A. N. Other"/>', [personInstance: personInstance]) == "A. N. Other"
	}

	void "default attribute is ignored if property has non-null value"() {
		given:
		views["/forms/default/_field.gsp"] = '${value}'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name" default="A. N. Other"/>', [personInstance: personInstance]) == "Bart Simpson"
	}

	void "errors passed to template is an empty collection for valid bean"() {
		given:
		views["/forms/default/_field.gsp"] = '<g:each var="error" in="${errors}"><em>${error}</em></g:each>'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == ""
	}

	void "errors passed to template is a collection of strings"() {
		given:
		views["/forms/default/_field.gsp"] = '<g:each var="error" in="${errors}"><em>${error}</em></g:each>'

		and:
		personInstance.errors.rejectValue("name", "blank")
		personInstance.errors.rejectValue("name", "nullable")

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<em>blank</em><em>nullable</em>"
	}

	void "resolves template for embedded class property"() {
		given:
		views["/forms/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/forms/string/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/forms/address/city/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'

		expect:
		applyTemplate('<form:field bean="personInstance" property="address.city"/>', [personInstance: personInstance]) == "CLASS AND PROPERTY TEMPLATE"
	}

	void "required flag is passed to template"() {
		given:
		views["/forms/default/_field.gsp"] = 'required=${required}'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "required=true"
	}

	void "required flag can be forced with attribute"() {
		given:
		views["/forms/default/_field.gsp"] = 'required=${required}'

		expect:
		applyTemplate('<form:field bean="personInstance" property="minor" required="true"/>', [personInstance: personInstance]) == "required=true"
	}

	void "required flag can be forced off with attribute"() {
		given:
		views["/forms/default/_field.gsp"] = 'required=${required}'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name" required="false"/>', [personInstance: personInstance]) == "required=false"
	}

	void "invalid flag is passed to template if bean has errors"() {
		given:
		views["/forms/default/_field.gsp"] = 'invalid=${invalid}'

		and:
		personInstance.errors.rejectValue("name", "blank")

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "invalid=true"
	}

	void "invalid flag is not passed to template if bean has no errors"() {
		given:
		views["/forms/default/_field.gsp"] = 'invalid=${invalid}'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "invalid=false"
	}

	void "invalid flag can be overridden with attribute"() {
		given:
		views["/forms/default/_field.gsp"] = 'invalid=${invalid}'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name" invalid="true"/>', [personInstance: personInstance]) == "invalid=true"
	}

	void "rendered input is passed to template"() {
		given:
		views["/forms/default/_field.gsp"] = '${widget}'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == '<input type="text" name="name" value="Bart Simpson" required="" id="name" />'
	}

	void "rendered input is overridden by template for property type"() {
		given:
		views["/forms/default/_field.gsp"] = '${widget}'
		views["/forms/string/_input.gsp"] = 'PROPERTY TYPE TEMPLATE'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'PROPERTY TYPE TEMPLATE'
	}

	void "rendered input is overridden by template for domain class property"() {
		given:
		views["/forms/default/_field.gsp"] = '${widget}'
		views["/forms/string/_input.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/forms/person/name/_input.gsp"] = 'CLASS AND PROPERTY TEMPLATE'

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "CLASS AND PROPERTY TEMPLATE"
	}

	void "rendered input is overridden by template from controller views directory"() {
		given:
		views["/forms/default/_field.gsp"] = '${widget}'
		views["/forms/string/_input.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/forms/person/name/_input.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
		views["/person/name/_input.gsp"] = 'CONTROLLER INPUT TEMPLATE'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'CONTROLLER INPUT TEMPLATE'
	}

	void "bean tag renders fields for all properties"() {
		given:
		views["/forms/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<form:bean bean="personInstance"/>', [personInstance: personInstance])

		then:
		output =~ /\bname\b/
		output =~ /\bpassword\b/
		output =~ /\bgender\b/
		output =~ /\bdateOfBirth\b/
		output =~ /\bminor\b/
	}

	void "bean tag renders individual fields for embedded properties"() {
		given:
		views["/forms/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<form:bean bean="personInstance"/>', [personInstance: personInstance])

		then:
		output.contains('address.street address.city address.country')
	}

	void "bean tag wraps embedded properties in a container"() {
		given:
		views["/forms/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<form:bean bean="personInstance"/>', [personInstance: personInstance])

		then:
		output.contains('<fieldset class="address"><legend>Address</legend>address.street address.city address.country </fieldset>')
	}
	
	void 'bean tag skips properties excluded by a static scaffold property in the domain class'() {
		given:
		views["/forms/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<form:bean bean="personInstance"/>', [personInstance: personInstance])

		then:
		!output.contains('excludedProperty')
	}

	void "bean tag skips event properties"() {
		given:
		views["/forms/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<form:bean bean="personInstance"/>', [personInstance: personInstance])

		then:
		!output.contains("onLoad")
	}

	void "bean tag skips timestamp properties"() {
		given:
		views["/forms/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<form:bean bean="personInstance"/>', [personInstance: personInstance])

		then:
		!output.contains("lastUpdated")
	}

}

@Entity
class Person {
    SalutationEnum salutation
	String name
	String password
	String gender
	Date dateOfBirth
	Address address
	boolean minor
	Date lastUpdated
	String excludedProperty

	static embedded = ['address']

	static constraints = {
        salutation nullable: true
		name blank: false
		address nullable: true
	}

	static scaffold = [exclude: ['excludedProperty']]

	def onLoad = {
		println "loaded"
	}
}

@Entity
class Employee extends Person {
	int salary
}

class Address {
	String street
	String city
	String country
	static constraints = {
		street blank: false
		city blank: false
		country inList: ["USA", "UK", "Canada"]
	}
}

enum SalutationEnum {
    MR,
    MRS
}
