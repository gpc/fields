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

		mockFormFieldsTemplateService.findTemplate(_, 'wrapper', null, null) >> [path: '/_fields/default/wrapper']
        mockFormFieldsTemplateService.getTemplateFor('wrapper') >> "wrapper"
        mockFormFieldsTemplateService.getTemplateFor('widget') >> "widget"
        mockFormFieldsTemplateService.getTemplateFor('displayWrapper') >> "displayWrapper"
        mockFormFieldsTemplateService.getTemplateFor('displayWidget') >> "displayWidget"
		mockFormFieldsTemplateService.getWidgetPrefix() >> 'input-'
		taglib.formFieldsTemplateService = mockFormFieldsTemplateService

        mockEmbeddedSitemeshLayout taglib
    }

	void "field tag renders individual fields for embedded properties"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:field bean="personInstance" property="address"/>', [personInstance: personInstance])

		then:
		output.contains('address.street address.city address.country')
	}

	void "field tag wraps embedded properties in a container"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = '${property} '

		expect:
		applyTemplate('<f:field bean="personInstance" property="address"/>', [personInstance: personInstance]) == '<fieldset class="embedded address"><legend>Address</legend>address.street address.city address.country </fieldset>'
	}

	void "embedded property label is resolved from message bundle"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = '${property} '

		and:
		messageSource.addMessage('person.address.label', request.locale, 'Address of person')

		when:
		def output = applyTemplate('<f:field bean="personInstance" property="address"/>', [personInstance: personInstance])

		then:
		output.contains('<legend>Address of person</legend>')
	}

}