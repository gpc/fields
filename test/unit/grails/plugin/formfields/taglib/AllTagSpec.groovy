package grails.plugin.formfields.taglib

import grails.plugin.formfields.mock.Person
import grails.plugin.formfields.*
import grails.test.mixin.*
import spock.lang.*

@TestFor(FormFieldsTagLib)
@Mock(Person)
@Unroll
class AllTagSpec extends AbstractFormFieldsTagLibSpec {

	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

	def setupSpec() {
		configurePropertyAccessorSpringBean()
	}

	def setup() {
		def taglib = applicationContext.getBean(FormFieldsTagLib)

		views["/_fields/_layouts/_noLayout.gsp"] = '${raw(renderedField)}'
		mockFormFieldsTemplateService.findTemplate(_, 'field', null) >> [path: '/_fields/default/field']
		mockFormFieldsTemplateService.findTemplateByPath(_) >> null
		taglib.formFieldsTemplateService = mockFormFieldsTemplateService

		mockEmbeddedSitemeshLayout(taglib)
	}

	void "all tag renders fields for all properties"() {
		given:
		views["/_fields/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:all bean="personInstance"/>', [personInstance: personInstance])

		then:
		output =~ /\bname\b/
		output =~ /\bpassword\b/
		output =~ /\bgender\b/
		output =~ /\bdateOfBirth\b/
		output =~ /\bminor\b/
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/21')
	void 'all tag skips #property property'() {
		given:
		views["/_fields/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:all bean="personInstance"/>', [personInstance: personInstance])

		then:
		!output.contains(property)

		where:
		property << ['id', 'version', 'onLoad', 'lastUpdated', 'excludedProperty', 'displayFalseProperty']
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/12')
	void 'all tag skips properties listed with the except attribute'() {
		given:
		views["/_fields/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:all bean="personInstance" except="password, minor"/>', [personInstance: personInstance])

		then:
		!output.contains('password')
		!output.contains('minor')
	}

}