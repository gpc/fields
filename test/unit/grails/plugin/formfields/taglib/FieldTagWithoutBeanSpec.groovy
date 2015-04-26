package grails.plugin.formfields.taglib

import grails.test.mixin.TestFor
import spock.lang.Issue
import grails.plugin.formfields.*

@Issue('https://github.com/grails-fields-plugin/grails-fields/pull/16')
@TestFor(FormFieldsTagLib)
class FieldTagWithoutBeanSpec extends AbstractFormFieldsTagLibSpec {

	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

	def setupSpec() {
		configurePropertyAccessorSpringBean()
	}

	def setup() {
		def taglib = applicationContext.getBean(FormFieldsTagLib)

		mockFormFieldsTemplateService.findTemplate(_, 'wrapper', null) >> [path: '/_fields/default/wrapper']
        mockFormFieldsTemplateService.getTemplateFor('wrapper') >> "wrapper"
        mockFormFieldsTemplateService.getTemplateFor('widget') >> "widget"
        mockFormFieldsTemplateService.getTemplateFor('displayWrapper') >> "displayWrapper"
        mockFormFieldsTemplateService.getTemplateFor('displayWidget') >> "displayWidget"
		taglib.formFieldsTemplateService = mockFormFieldsTemplateService
	}

	void 'f:field can work without a bean attribute'() {
		given:
		views["/_fields/default/_wrapper.gsp"] = '${property}'

		expect:
		applyTemplate('<f:field property="name"/>') == 'name'
	}

	void 'label is the natural property name if there is no bean attribute'() {
		given:
		views["/_fields/default/_wrapper.gsp"] = '${label}'

		expect:
		applyTemplate('<f:field property="name"/>') == 'Name'
	}

}