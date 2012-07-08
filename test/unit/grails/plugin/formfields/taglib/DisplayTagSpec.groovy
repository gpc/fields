package grails.plugin.formfields.taglib

import grails.test.mixin.TestFor
import spock.lang.Issue
import grails.plugin.formfields.*

@Issue('https://github.com/robfletcher/grails-fields/issues/45')
@TestFor(FormFieldsTagLib)
class DisplayTagSpec extends AbstractFormFieldsTagLibSpec {

	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

	def setupSpec() {
		configurePropertyAccessorSpringBean()
	}

	def setup() {
		def taglib = applicationContext.getBean(FormFieldsTagLib)

		taglib.formFieldsTemplateService = mockFormFieldsTemplateService
	}

	void 'displays using template if one is present'() {
		given:
		views["/_fields/default/_display.gsp"] = '<dt>${label}</dt><dd>${value}</dd>'

		and:
		mockFormFieldsTemplateService.findTemplate(_, 'display') >> [path: '/_fields/default/display']

		expect:
		applyTemplate('<f:display bean="personInstance" property="name"/>', [personInstance: personInstance]) == '<dt>Name</dt><dd>Bart Simpson</dd>'
	}

	void 'displays using g:fieldValue if no template is present'() {
		expect:
		applyTemplate('<f:display bean="personInstance" property="name"/>', [personInstance: personInstance]) == personInstance.name
	}

	void 'displays boolean values using g:formatBoolean'() {
		given:
		messageSource.addMessage('default.boolean.true', request.locale, 'Yes')

		expect:
		applyTemplate('<f:display bean="personInstance" property="minor"/>', [personInstance: personInstance]) == 'Yes'
	}

	void 'displays date values using g:formatDate'() {
		expect:
		applyTemplate('<f:display bean="personInstance" property="dateOfBirth"/>', [personInstance: personInstance]) == '1987-04-19 00:00:00 BST'
	}

}
