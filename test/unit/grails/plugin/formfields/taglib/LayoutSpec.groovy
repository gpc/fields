package grails.plugin.formfields.taglib

import grails.plugin.formfields.FormFieldsTagLib
import grails.plugin.formfields.FormFieldsTemplateService
import grails.plugin.formfields.mock.Employee
import grails.plugin.formfields.mock.Person
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Issue
import spock.lang.Unroll

@TestFor(FormFieldsTagLib)
@Mock([Person, Employee])
@Unroll
class LayoutSpec extends AbstractFormFieldsTagLibSpec {

	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

	def setupSpec() {
		configurePropertyAccessorSpringBean()
	}

	def setup() {
		def taglib = applicationContext.getBean(FormFieldsTagLib)

		views["/_fields/default/_field.gsp"] = 'Field'
		views["/_fields/default/_display.gsp"] = 'Display'
		views["/_fields/default/_input.gsp"] = 'Input'
		views["/_fields/_layouts/_layoutA.gsp"] = 'layoutA[${raw(renderedField)}]'
		views["/_fields/_layouts/_layoutB.gsp"] = 'layoutB[${raw(renderedField)}]'
		mockFormFieldsTemplateService.findTemplate(_, 'field', null) >> [path: '/_fields/default/field']
		mockFormFieldsTemplateService.findTemplate(_, 'display', null) >> [path: '/_fields/default/display']
		mockFormFieldsTemplateService.findTemplate(_, 'input', null) >> [path: '/_fields/default/input']
		taglib.formFieldsTemplateService = mockFormFieldsTemplateService
	}

	void "render specific layout for field"() {
		given:
		mockFormFieldsTemplateService.findLayout(_, null, _) >> [path: '/_fields/_layouts/layoutA']

		expect:
		applyTemplate('<f:field bean="personInstance" property="name" layout="layoutA"/>', [personInstance: personInstance]) == "layoutA[Field]"
	}

	void "no render layout for input"() {
		when:
		def result = applyTemplate('<f:input bean="personInstance" property="name" layout="layoutA"/>', [personInstance: personInstance])

		then:
		0 * mockFormFieldsTemplateService.findLayout(_, null, _)
		result == "Input"
	}

	void "render specific layout for display"() {
		given:
		mockFormFieldsTemplateService.findLayout(_, null, _) >> [path: '/_fields/_layouts/layoutA']

		expect:
		applyTemplate('<f:display bean="personInstance" property="name" layout="layoutA"/>', [personInstance: personInstance]) == "layoutA[Display]"
	}

}
