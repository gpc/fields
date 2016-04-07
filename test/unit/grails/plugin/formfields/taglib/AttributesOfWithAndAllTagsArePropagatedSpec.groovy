package grails.plugin.formfields.taglib

import grails.plugin.formfields.FormFieldsTagLib
import grails.plugin.formfields.FormFieldsTemplateService
import grails.plugin.formfields.mock.Employee
import grails.plugin.formfields.mock.Person
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Unroll

@TestFor(FormFieldsTagLib)
@Mock([Person, Employee])
@Unroll
@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/210')
class AttributesOfWithAndAllTagsArePropagatedSpec extends AbstractFormFieldsTagLibSpec {

	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

	def setupSpec() {
		configurePropertyAccessorSpringBean()
	}

	def setup() {
		def taglib = applicationContext.getBean(FormFieldsTagLib)

		mockFormFieldsTemplateService.getWidgetPrefix() >> "widget-"

		mockFormFieldsTemplateService.findTemplate(_, 'wrapper', null) >> [path: '/_fields/default/wrapper']
		mockFormFieldsTemplateService.findTemplate(_, 'displayWrapper', null) >> [path: '/_fields/default/displayWrapper']

		mockFormFieldsTemplateService.findTemplate(_, 'widget', null) >> [path: '/_fields/default/widget']
		mockFormFieldsTemplateService.findTemplate(_, 'displayWidget', null) >> [path: '/_fields/default/displayWidget']

		mockFormFieldsTemplateService.findTemplate(_, 'wrapper', "cool") >> [path: '/_fields/cool/wrapper']
		mockFormFieldsTemplateService.findTemplate(_, 'displayWrapper', "cool") >> [path: '/_fields/cool/displayWrapper']

		mockFormFieldsTemplateService.getTemplateFor('wrapper') >> "wrapper"
		mockFormFieldsTemplateService.getTemplateFor('widget') >> "widget"
		mockFormFieldsTemplateService.getTemplateFor('displayWrapper') >> "displayWrapper"
		mockFormFieldsTemplateService.getTemplateFor('displayWidget') >> "displayWidget"

		taglib.formFieldsTemplateService = mockFormFieldsTemplateService

		mockEmbeddedSitemeshLayout(taglib)

		views["/_fields/_embedded.gsp"] = '<g:layoutBody/>'
		views["/_fields/default/_wrapper.gsp"] = '<wrapper>${widget}</wrapper>'
		views["/_fields/default/_displayWrapper.gsp"] = '<displayWrapper>${widget}</displayWrapper>'
		views["/_fields/default/_widget.gsp"] = 'widget'
		views["/_fields/default/_displayWidget.gsp"] = 'displayWidget'
		views["/_fields/cool/_wrapper.gsp"] = '<coolWrapper>${widget}</coolWrapper>'
		views["/_fields/cool/_displayWrapper.gsp"] = '<coolDisplayWrapper>${widget}</coolDisplayWrapper>'
		views["/_fields/cool/_widget.gsp"] = 'coolWidget'
		views["/_fields/cool/_displayWidget.gsp"] = 'coolDisplayWidget'
	}

	void "An attribute of the <f:with> tag is present on a field tag"() {
		expect:
		applyTemplate(
				'<f:with bean="personInstance" wrapper="cool">' +
					'<f:field property="name"/>' +
				'</f:with>',
				[personInstance: personInstance]
		) == "<coolWrapper>widget</coolWrapper>"
	}

	void "An attribute of the <f:with> tag is present on an inner display tag"() {
		expect:
		applyTemplate(
				'<f:with bean="personInstance" wrapper="cool">' +
					'<f:display property="name"/>' +
				'</f:with>',
				[personInstance: personInstance]
		) == "<coolDisplayWrapper>displayWidget</coolDisplayWrapper>"
	}

	void "An attribute of the <f:all> tag is present on inner fields tag"() {
		expect:
		applyTemplate(
				'<f:all bean="personInstance" wrapper="cool"/>',
				[personInstance: personInstance]
		) == "<coolWrapper>widget</coolWrapper>" * 15 //15 is the number of properties of the person class
	}

	@Ignore("It is not possible to use the <f:all> tag for printing displays instead of fields")
	void "An attribute of the <f:displayAll> tag is present on inner displays tag"() {
		expect:
		applyTemplate(
				'<f:displayAll bean="personInstance" wrapper="cool"/>',
				[personInstance: personInstance]
		) == "<coolWrapper>displayWidget</coolWrapper>" * 15 //15 is the number of properties of the person class
	}

	//Test Override of attributes

	void "<f:field> attributes overrides <f:with> attributes"() {
		given:
		views["/_fields/default/_widget.gsp"] = '<div class="${someClass}">widget</div>'

		expect:
		applyTemplate(
				'<f:with bean="personInstance" widget-someClass="general-class">' +
					'<f:field property="name" widget-someClass="override-class"/>' +
				'</f:with>',
				[personInstance: personInstance]
		) == '<wrapper><div class="override-class">widget</div></wrapper>'
	}

	void "<f:display> attributes overrides <f:with> attributes"() {
		given:
		views["/_fields/default/_displayWidget.gsp"] = '<div class="${someClass}">displayWidget</div>'

		expect:
		applyTemplate(
				'<f:with bean="personInstance" widget-someClass="general-class">' +
						'<f:display property="name" widget-someClass="override-class"/>' +
				'</f:with>',
				[personInstance: personInstance]
		) == '<displayWrapper><div class="override-class">displayWidget</div></displayWrapper>'
	}

	//Test cascade tags

	void "Test cascade of <f:with> tags"() {
		given:
		views["/_fields/default/_widget.gsp"] = '<div class="${someClass}">${property}</div>'

		expect:
		applyTemplate(
				'<f:with bean="personInstance" wrapper="cool" widget-someClass="defaultWidgetClass">' +
					'<f:field property="name"/>' +
					'<f:with bean="personInstance" wrapper="default">' +
						'<f:field property="password"/>' +
					'</f:with>' +
				'</f:with>',
				[personInstance: personInstance]
		) == '<coolWrapper><div class="defaultWidgetClass">name</div></coolWrapper>' +
			 '<wrapper><div class="defaultWidgetClass">password</div></wrapper>'
	}

}
