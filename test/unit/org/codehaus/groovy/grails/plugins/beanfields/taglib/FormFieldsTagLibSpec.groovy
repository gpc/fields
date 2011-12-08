package org.codehaus.groovy.grails.plugins.beanfields.taglib

import grails.persistence.Entity
import org.codehaus.groovy.grails.plugins.beanfields.BeanPropertyAccessorFactory
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import spock.lang.Specification
import grails.test.mixin.*

@TestFor(FormFieldsTagLib)
@Mock(Person)
class FormFieldsTagLibSpec extends Specification {

	def personInstance
	def resourceLoader

	def setupSpec() {
		applicationContext.registerSingleton("beanPropertyAccessorFactory", BeanPropertyAccessorFactory)
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
		views["/forms/String/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'PROPERTY TYPE TEMPLATE'
	}

	void "resolves template for domain class property"() {
		given:
		views["/forms/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/forms/String/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/forms/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "CLASS AND PROPERTY TEMPLATE"
	}

	void "resolves template from controller views directory"() {
		given:
		views["/forms/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
		views["/forms/String/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
		views["/forms/person/name/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
		views["/person/name/_field.gsp"] = 'CONTROLLER FIELD TEMPLATE'

		expect:
		applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'CONTROLLER FIELD TEMPLATE'
	}
	
	// TODO: superclasses

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

		expect
		applyTemplate('<form:field bean="personInstance" property="name" label="Name of person"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
	}

//	void testLabelCanBeOverriddenByLabelKeyAttribute() {
//		views["/forms/default/_field.gsp"] = '<label>${label}</label>'
//		messageSource.addMessage("custom.name.label", RequestContextUtils.getLocale(request), "Name of person")
//
//		assert applyTemplate('<form:field bean="personInstance" property="name" labelKey="custom.name.label"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
//	}
//
//	void testValueIsDefaultedToPropertyValue() {
//		views["/forms/default/_field.gsp"] = '<g:formatDate date="${value}" format="yyyy-MM-dd"/>'
//
//		assert applyTemplate('<form:field bean="personInstance" property="dateOfBirth"/>', [personInstance: personInstance]) == "1987-04-19"
//	}
//
//	void testValueIsOverriddenByValueAttribute() {
//		views["/forms/default/_field.gsp"] = '${value}'
//
//		assert applyTemplate('<form:field bean="personInstance" property="name" value="Bartholomew J. Simpson"/>', [personInstance: personInstance]) == "Bartholomew J. Simpson"
//	}
//
//	void testValueFallsBackToDefault() {
//		views["/forms/default/_field.gsp"] = '${value}'
//		personInstance.name = null
//
//		assert applyTemplate('<form:field bean="personInstance" property="name" default="A. N. Other"/>', [personInstance: personInstance]) == "A. N. Other"
//	}
//
//	void testDefaultAttributeIsIgnoredIfPropertyHasNonNullValue() {
//		views["/forms/default/_field.gsp"] = '${value}'
//
//		assert applyTemplate('<form:field bean="personInstance" property="name" default="A. N. Other"/>', [personInstance: personInstance]) == "Bart Simpson"
//	}
//
//	void testErrorsPassedToTemplateIsAnEmptyCollectionForValidBean() {
//		views["/forms/default/_field.gsp"] = '<g:each var="error" in="${errors}"><em>${error}</em></g:each>'
//
//		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == ""
//	}
//
//	void testErrorsPassedToTemplateIsAnCollectionOfStrings() {
//		views["/forms/default/_field.gsp"] = '<g:each var="error" in="${errors}"><em>${error}</em></g:each>'
//		personInstance.errors.rejectValue("name", "blank")
//		personInstance.errors.rejectValue("name", "nullable")
//
//		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<em>blank</em><em>nullable</em>"
//	}
//
//	void testResolvesTemplateForEmbeddedClassProperty() {
//		views["/forms/default/_field.gsp"] = 'DEFAULT FIELD TEMPLATE'
//		views["/forms/java.lang.String/_field.gsp"] = 'PROPERTY TYPE TEMPLATE'
//		views["/forms/address/city/_field.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
//
//		assert applyTemplate('<form:field bean="personInstance" property="address.city"/>', [personInstance: personInstance]) == "CLASS AND PROPERTY TEMPLATE"
//	}
//
//	void testRequiredFlagIsPassedToTemplate() {
//		views["/forms/default/_field.gsp"] = 'required=${required}'
//
//		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "required=true"
//	}
//
//	void testRequiredFlagCanBeForcedWithAttribute() {
//		views["/forms/default/_field.gsp"] = 'required=${required}'
//
//		assert applyTemplate('<form:field bean="personInstance" property="minor" required="true"/>', [personInstance: personInstance]) == "required=true"
//	}
//
//	void testRequiredFlagCanBeForcedOffWithAttribute() {
//		views["/forms/default/_field.gsp"] = 'required=${required}'
//
//		assert applyTemplate('<form:field bean="personInstance" property="name" required="false"/>', [personInstance: personInstance]) == "required=false"
//	}
//
//	void testInvalidFlagIsPassedToTemplateIfBeanHasErrors() {
//		views["/forms/default/_field.gsp"] = 'invalid=${invalid}'
//		personInstance.errors.rejectValue("name", "blank")
//
//		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "invalid=true"
//	}
//
//	void testInvalidFlagIsNotPassedToTemplateIfBeanHasNoErrors() {
//		views["/forms/default/_field.gsp"] = 'invalid=${invalid}'
//
//		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "invalid=false"
//	}
//
//	void testInvalidFlagCanBeOverriddenWithAttribute() {
//		views["/forms/default/_field.gsp"] = 'invalid=${invalid}'
//
//		assert applyTemplate('<form:field bean="personInstance" property="name" invalid="true"/>', [personInstance: personInstance]) == "invalid=true"
//	}
//
//	void testRenderedInputIsPassedToTemplate() {
//		views["/forms/default/_field.gsp"] = '${input}'
//
//		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == '<input type="text" name="name" value="Bart Simpson" required="" id="name" />'
//	}
//
//	void testRenderedInputIsOverriddenByTemplateForPropertyType() {
//		views["/forms/default/_field.gsp"] = '${input}'
//
//		views["/forms/java.lang.String/_input.gsp"] = 'PROPERTY TYPE TEMPLATE'
//
//		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'PROPERTY TYPE TEMPLATE'
//	}
//
//	void testRenderedInputIsOverriddenByTemplateForDomainClassProperty() {
//		views["/forms/default/_field.gsp"] = '${input}'
//
//		views["/forms/java.lang.String/_input.gsp"] = 'PROPERTY TYPE TEMPLATE'
//		views["/forms/person/name/_input.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
//
//		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "CLASS AND PROPERTY TEMPLATE"
//	}
//
//	void testRenderedInputIsOverriddenByTemplateFromControllerViewsDirectory() {
//		views["/forms/default/_field.gsp"] = '${input}'
//
//		views["/forms/java.lang.String/_input.gsp"] = 'PROPERTY TYPE TEMPLATE'
//		views["/forms/person/name/_input.gsp"] = 'CLASS AND PROPERTY TEMPLATE'
//		views["/person/name/_input.gsp"] = 'CONTROLLER FIELD TEMPLATE'
//
//		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'CONTROLLER FIELD TEMPLATE'
//	}
//
//	void testBeanTagRendersFieldsForAllProperties() {
//		views["/forms/default/_field.gsp"] = '${property} '
//
//		def output = applyTemplate('<form:bean bean="personInstance"/>', [personInstance: personInstance])
//		output =~ /\bname\b/
//		output =~ /\bpassword\b/
//		output =~ /\bgender\b/
//		output =~ /\bdateOfBirth\b/
//		output =~ /\bminor\b/
//	}
//
//	void testBeanTagRendersIndividualFieldsForEmbeddedProperties() {
//		views["/forms/default/_field.gsp"] = '${property} '
//
//		def output = applyTemplate('<form:bean bean="personInstance"/>', [personInstance: personInstance])
//		output =~ /\baddress\.street\b/
//		output =~ /\baddress\.city\b/
//		output =~ /\baddress\.country\b/
//	}
//
//	void testBeanTagSkipsEventProperties() {
//		views["/forms/default/_field.gsp"] = '${property} '
//
//		def output = applyTemplate('<form:bean bean="personInstance"/>', [personInstance: personInstance])
//		!output.contains("onLoad")
//	}
//
//	void testBeanTagSkipsTimestampProperties() {
//		views["/forms/default/_field.gsp"] = '${property} '
//
//		def output = applyTemplate('<form:bean bean="personInstance"/>', [personInstance: personInstance])
//		!output.contains("lastUpdated")
//	}

}

@Entity
class Person {
	String name
	String password
	String gender
	Date dateOfBirth
	Address address
	boolean minor
	Date lastUpdated

	static embedded = ['address']

	static constraints = {
		name blank: false
		address nullable: true
	}

	def onLoad = {
		println "loaded"
	}
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
