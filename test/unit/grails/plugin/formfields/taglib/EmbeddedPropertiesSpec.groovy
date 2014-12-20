package grails.plugin.formfields.taglib

import grails.plugin.formfields.mock.Person
import spock.lang.Issue
import grails.plugin.formfields.*
import grails.test.mixin.*

@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/30')
@TestFor(FormFieldsTagLib)
@Mock(Person)
class EmbeddedPropertiesSpec extends AbstractFormFieldsTagLibSpec {

	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

	def setupSpec() {
		configurePropertyAccessorSpringBean()
	}

	def setup() {
		def taglib = applicationContext.getBean(FormFieldsTagLib)

		views["/_fields/_layouts/_noLayout.gsp"] = '${raw(renderedField)}'
		mockFormFieldsTemplateService.findTemplate(_, 'field', null) >> [path: '/_fields/default/field']
		taglib.formFieldsTemplateService = mockFormFieldsTemplateService

        mockEmbeddedSitemeshLayout taglib
    }

	void "field tag renders individual fields for embedded properties"() {
		given:
		views["/_fields/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:field bean="personInstance" property="address"/>', [personInstance: personInstance])

		then:
		output.contains('address.street address.city address.country')
	}

	void "field tag wraps embedded properties in a container"() {
		given:
		views["/_fields/default/_field.gsp"] = '${property} '

		expect:
		applyTemplate('<f:field bean="personInstance" property="address"/>', [personInstance: personInstance]) == '<fieldset class="embedded address"><legend>Address</legend>address.street address.city address.country </fieldset>'
	}

	void "embedded property label is resolved from message bundle"() {
		given:
		views["/_fields/default/_field.gsp"] = '${property} '

		and:
		messageSource.addMessage('person.address.label', request.locale, 'Address of person')

		when:
		def output = applyTemplate('<f:field bean="personInstance" property="address"/>', [personInstance: personInstance])

		then:
		output.contains('<legend>Address of person</legend>')
	}

}