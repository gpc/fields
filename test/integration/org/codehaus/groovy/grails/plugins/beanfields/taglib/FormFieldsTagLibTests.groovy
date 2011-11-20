package org.codehaus.groovy.grails.plugins.beanfields.taglib

import org.codehaus.groovy.grails.plugins.web.taglib.RenderTagLib
import org.codehaus.groovy.grails.support.MockStringResourceLoader
import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.springframework.web.servlet.support.RequestContextUtils
import org.codehaus.groovy.grails.web.taglib.*

class FormFieldsTagLibTests extends AbstractGrailsTagTests {

	def personInstance
	def resourceLoader

	protected void onSetUp() {
		gcl.parseClass '''
			@grails.persistence.Entity
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
		'''
	}

	@Override
	void setUp() {
		super.setUp()

		resourceLoader = new MockStringResourceLoader()
		appCtx.groovyPagesTemplateEngine.resourceLoader = resourceLoader

		webRequest.controllerName = "person"

		def person = ga.getDomainClass("Person")
		personInstance = person.clazz.newInstance(name: "Bart Simpson", password: "bartman", gender: "Male", dateOfBirth: new Date(87, 3, 19), minor: true)

		def address = ga.classLoader.loadClass("Address")
		personInstance.address = address.newInstance(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")
	}

	@Override
	void tearDown() {
		super.tearDown()

		RenderTagLib.TEMPLATE_CACHE.clear()
	}

	void testBeanAttributeIsRequired() {
		shouldFail(GrailsTagException) {
			applyTemplate('<form:field property="name"/>')
		}
	}

	void testPropertyAttributeIsRequired() {
		shouldFail(GrailsTagException) {
			applyTemplate('<form:field bean="${personInstance}"/>', [personInstance: personInstance])
		}
	}

	void testBeanAttributeMustBeNonNull() {
		shouldFail(GrailsTagException) {
			applyTemplate('<form:field bean="${personInstance}" property="name"/>', [personInstance: null])
		}
	}

	void testBeanAttributeCanBeAString() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '${bean.getClass().name}')

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "Person"
	}

	void testBeanAttributeStringMustReferToVariableInPage() {
		shouldFail(GrailsTagException) {
			applyTemplate('<form:field bean="personInstance" property="name"/>')
		}
	}

	void testUsesDefaultTemplate() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", 'DEFAULT FIELD TEMPLATE')

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'DEFAULT FIELD TEMPLATE'
	}

	void testResolvesTemplateForPropertyType() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", 'DEFAULT FIELD TEMPLATE')
		resourceLoader.registerMockResource("/grails-app/views/forms/java.lang.String/_field.gsp", 'PROPERTY TYPE TEMPLATE')

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'PROPERTY TYPE TEMPLATE'
	}

	void testResolvesTemplateForDomainClassProperty() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", 'DEFAULT FIELD TEMPLATE')
		resourceLoader.registerMockResource("/grails-app/views/forms/java.lang.String/_field.gsp", 'PROPERTY TYPE TEMPLATE')
		resourceLoader.registerMockResource("/grails-app/views/forms/person/name/_field.gsp", 'CLASS AND PROPERTY TEMPLATE')

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "CLASS AND PROPERTY TEMPLATE"
	}

	void testResolvesTemplateFromControllerViewsDirectory() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", 'DEFAULT FIELD TEMPLATE')
		resourceLoader.registerMockResource("/grails-app/views/forms/java.lang.String/_field.gsp", 'PROPERTY TYPE TEMPLATE')
		resourceLoader.registerMockResource("/grails-app/views/forms/person/name/_field.gsp", 'CLASS AND PROPERTY TEMPLATE')
		resourceLoader.registerMockResource("/grails-app/views/person/name/_field.gsp", 'CONTROLLER FIELD TEMPLATE')

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'CONTROLLER FIELD TEMPLATE'
	}

	void testBeanAndPropertyAttributesArePassedToTemplate() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '${bean.getClass().name}.${property}')

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "Person.name"
	}

	void testConstraintsArePassedToTemplate() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", 'nullable=${constraints.nullable}, blank=${constraints.blank}')

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "nullable=false, blank=false"
	}

	void testLabelIsResolvedByConventionAndPassedToTemplate() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '<label>${label}</label>')
		messageSource.addMessage("Person.name.label", RequestContextUtils.getLocale(request), "Name of person")

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
	}

	void testLabelIsDefaultedToNaturalPropertyName() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '<label>${label}</label>')

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<label>Name</label>"
		assert applyTemplate('<form:field bean="personInstance" property="dateOfBirth"/>', [personInstance: personInstance]) == "<label>Date Of Birth</label>"
	}

	void testLabelCanBeOverriddenByLabelAttribute() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '<label>${label}</label>')

		assert applyTemplate('<form:field bean="personInstance" property="name" label="Name of person"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
	}

	void testLabelCanBeOverriddenByLabelKeyAttribute() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '<label>${label}</label>')
		messageSource.addMessage("custom.name.label", RequestContextUtils.getLocale(request), "Name of person")

		assert applyTemplate('<form:field bean="personInstance" property="name" labelKey="custom.name.label"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
	}

	void testValueIsDefaultedToPropertyValue() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '<g:formatDate date="${value}" format="yyyy-MM-dd"/>')

		assert applyTemplate('<form:field bean="personInstance" property="dateOfBirth"/>', [personInstance: personInstance]) == "1987-04-19"
	}

	void testValueIsOverriddenByValueAttribute() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '${value}')

		assert applyTemplate('<form:field bean="personInstance" property="name" value="Bartholomew J. Simpson"/>', [personInstance: personInstance]) == "Bartholomew J. Simpson"
	}

	void testValueFallsBackToDefault() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '${value}')
		personInstance.name = null

		assert applyTemplate('<form:field bean="personInstance" property="name" default="A. N. Other"/>', [personInstance: personInstance]) == "A. N. Other"
	}

	void testDefaultAttributeIsIgnoredIfPropertyHasNonNullValue() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '${value}')

		assert applyTemplate('<form:field bean="personInstance" property="name" default="A. N. Other"/>', [personInstance: personInstance]) == "Bart Simpson"
	}

	void testErrorsPassedToTemplateIsAnEmptyCollectionForValidBean() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '<g:each var="error" in="${errors}"><em>${error}</em></g:each>')

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == ""
	}

	void testErrorsPassedToTemplateIsAnCollectionOfStrings() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '<g:each var="error" in="${errors}"><em>${error}</em></g:each>')
		personInstance.errors.rejectValue("name", "blank")
		personInstance.errors.rejectValue("name", "nullable")

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<em>blank</em><em>nullable</em>"
	}

	void testResolvesTemplateForEmbeddedClassProperty() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", 'DEFAULT FIELD TEMPLATE')
		resourceLoader.registerMockResource("/grails-app/views/forms/java.lang.String/_field.gsp", 'PROPERTY TYPE TEMPLATE')
		resourceLoader.registerMockResource("/grails-app/views/forms/address/city/_field.gsp", 'CLASS AND PROPERTY TEMPLATE')

		assert applyTemplate('<form:field bean="personInstance" property="address.city"/>', [personInstance: personInstance]) == "CLASS AND PROPERTY TEMPLATE"
	}

	void testRequiredFlagIsPassedToTemplate() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", 'required=${required}')

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "required=true"
	}

	void testRequiredFlagCanBeForcedWithAttribute() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", 'required=${required}')

		assert applyTemplate('<form:field bean="personInstance" property="minor" required="true"/>', [personInstance: personInstance]) == "required=true"
	}

	void testRequiredFlagCanBeForcedOffWithAttribute() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", 'required=${required}')

		assert applyTemplate('<form:field bean="personInstance" property="name" required="false"/>', [personInstance: personInstance]) == "required=false"
	}

	void testInvalidFlagIsPassedToTemplateIfBeanHasErrors() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", 'invalid=${invalid}')
		personInstance.errors.rejectValue("name", "blank")

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "invalid=true"
	}

	void testInvalidFlagIsNotPassedToTemplateIfBeanHasNoErrors() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", 'invalid=${invalid}')

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "invalid=false"
	}

	void testInvalidFlagCanBeOverriddenWithAttribute() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", 'invalid=${invalid}')

		assert applyTemplate('<form:field bean="personInstance" property="name" invalid="true"/>', [personInstance: personInstance]) == "invalid=true"
	}

	void testRenderedInputIsPassedToTemplate() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '${input}')

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == '<input type="text" name="name" value="Bart Simpson" required="" id="name" />'
	}

	void testRenderedInputIsOverriddenByTemplateForPropertyType() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '${input}')

		resourceLoader.registerMockResource("/grails-app/views/forms/java.lang.String/_input.gsp", 'PROPERTY TYPE TEMPLATE')

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'PROPERTY TYPE TEMPLATE'
	}

	void testRenderedInputIsOverriddenByTemplateForDomainClassProperty() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '${input}')

		resourceLoader.registerMockResource("/grails-app/views/forms/java.lang.String/_input.gsp", 'PROPERTY TYPE TEMPLATE')
		resourceLoader.registerMockResource("/grails-app/views/forms/person/name/_input.gsp", 'CLASS AND PROPERTY TEMPLATE')

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "CLASS AND PROPERTY TEMPLATE"
	}

	void testRenderedInputIsOverriddenByTemplateFromControllerViewsDirectory() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '${input}')

		resourceLoader.registerMockResource("/grails-app/views/forms/java.lang.String/_input.gsp", 'PROPERTY TYPE TEMPLATE')
		resourceLoader.registerMockResource("/grails-app/views/forms/person/name/_input.gsp", 'CLASS AND PROPERTY TEMPLATE')
		resourceLoader.registerMockResource("/grails-app/views/person/name/_input.gsp", 'CONTROLLER FIELD TEMPLATE')

		assert applyTemplate('<form:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'CONTROLLER FIELD TEMPLATE'
	}

	void testBeanTagRendersFieldsForAllProperties() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '${property} ')

		def output = applyTemplate('<form:bean bean="personInstance"/>', [personInstance: personInstance])
		output =~ /\bname\b/
		output =~ /\bpassword\b/
		output =~ /\bgender\b/
		output =~ /\bdateOfBirth\b/
		output =~ /\bminor\b/
	}

	void testBeanTagRendersIndividualFieldsForEmbeddedProperties() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '${property} ')

		def output = applyTemplate('<form:bean bean="personInstance"/>', [personInstance: personInstance])
		output =~ /\baddress\.street\b/
		output =~ /\baddress\.city\b/
		output =~ /\baddress\.country\b/
	}

	void testBeanTagSkipsEventProperties() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '${property} ')

		def output = applyTemplate('<form:bean bean="personInstance"/>', [personInstance: personInstance])
		!output.contains("onLoad")
	}

	void testBeanTagSkipsTimestampProperties() {
		resourceLoader.registerMockResource("/grails-app/views/forms/default/_field.gsp", '${property} ')

		def output = applyTemplate('<form:bean bean="personInstance"/>', [personInstance: personInstance])
		!output.contains("lastUpdated")
	}

}
