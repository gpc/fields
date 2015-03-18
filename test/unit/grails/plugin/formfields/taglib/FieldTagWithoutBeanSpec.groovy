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

		mockFormFieldsTemplateService.findTemplate(_, 'field', null) >> [path: '/_fields/default/field']
		taglib.formFieldsTemplateService = mockFormFieldsTemplateService
	}

	void 'f:field can work without a bean attribute'() {
		given:
		views["/_fields/default/_field.gsp"] = '${property}'

		expect:
		applyTemplate('<f:field property="name"/>') == 'name'
	}

	void 'label is the natural property name if there is no bean attribute'() {
		given:
		views["/_fields/default/_field.gsp"] = '${label}'

		expect:
		applyTemplate('<f:field property="name"/>') == 'Name'
	}

}