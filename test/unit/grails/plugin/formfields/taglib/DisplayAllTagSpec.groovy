package grails.plugin.formfields.taglib

import grails.plugin.formfields.FormFieldsTagLib
import grails.plugin.formfields.FormFieldsTemplateService
import grails.plugin.formfields.mock.Person
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Issue
import spock.lang.Unroll

@TestFor(FormFieldsTagLib)
@Mock(Person)
@Unroll
class DisplayAllTagSpec extends AbstractFormFieldsTagLibSpec {

	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

	def setupSpec() {
		configurePropertyAccessorSpringBean()
	}

	def setup() {
		def taglib = applicationContext.getBean(FormFieldsTagLib)

		mockFormFieldsTemplateService.findTemplate(_, 'displayWrapper', null) >> [path: '/_fields/default/displayWrapper']
        mockFormFieldsTemplateService.getTemplateFor('wrapper') >> "wrapper"
        mockFormFieldsTemplateService.getTemplateFor('widget') >> "widget"
        mockFormFieldsTemplateService.getTemplateFor('displayWrapper') >> "displayWrapper"
        mockFormFieldsTemplateService.getTemplateFor('displayWidget') >> "displayWidget"
		mockFormFieldsTemplateService.getWidgetPrefix() >> 'widget-'
		taglib.formFieldsTemplateService = mockFormFieldsTemplateService

		mockEmbeddedSitemeshLayout(taglib)
	}

	void "displayAll tag renders fields for all properties"() {
		given:
		views["/_fields/default/_displayWrapper.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:displayAll bean="personInstance"/>', [personInstance: personInstance])

		then:
		output =~ /\bname\b/
		output =~ /\bpassword\b/
		output =~ /\bgender\b/
		output =~ /\bdateOfBirth\b/
		output =~ /\bminor\b/
	}

	void 'displayAll tag skips #property property'() {
		given:
		views["/_fields/default/_displayWrapper.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:displayAll bean="personInstance"/>', [personInstance: personInstance])

		then:
		!output.contains(property)

		where:
		property << ['id', 'version', 'onLoad', 'lastUpdated', 'excludedProperty', 'displayFalseProperty']
	}

	void 'displayAll tag skips properties listed with the except attribute'() {
		given:
		views["/_fields/default/_displayWrapper.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:displayAll bean="personInstance" except="password, minor"/>', [personInstance: personInstance])

		then:
		!output.contains('password')
		!output.contains('minor')
	}

	void 'displayAll tag should render all the display for properties'() {
		given:
		mockFormFieldsTemplateService.findTemplate(_, 'displayWidget', null) >> [path: '/_fields/default/displayWidget']
		views["/_fields/default/_displayWrapper.gsp"] = '<displayWrapper>${widget}</displayWrapper>'
		views["/_fields/default/_displayWidget.gsp"] = '<displayWidget>${property}</displayWidget>'
		String prefix = "<displayWrapper><displayWidget>"
		String suffix = "</displayWidget></displayWrapper>"

		when:
		def output = applyTemplate('<f:displayAll bean="personInstance"/>', [personInstance: personInstance])

		then:
		output =~ /${prefix}name${suffix}/
		output =~ /${prefix}password${suffix}/
		output =~ /${prefix}gender${suffix}/
		output =~ /${prefix}dateOfBirth${suffix}/
		output =~ /${prefix}minor${suffix}/
	}
}