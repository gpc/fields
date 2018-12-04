package grails.plugin.formfields.taglib

import grails.plugin.formfields.mock.Person
import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Issue
import grails.plugin.formfields.*

@Issue('https://github.com/grails-fields-plugin/grails-fields/pull/16')
class FieldTagWithBodySpec extends AbstractFormFieldsTagLibSpec implements TagLibUnitTest<FormFieldsTagLib> {

	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

	def setupSpec() {
        mockDomain(Person)
	}

	def setup() {
		mockFormFieldsTemplateService.findTemplate(_, 'wrapper', null, null) >> [path: '/_fields/default/wrapper']
        mockFormFieldsTemplateService.getTemplateFor('wrapper') >> "wrapper"
        mockFormFieldsTemplateService.getTemplateFor('widget') >> "widget"
        mockFormFieldsTemplateService.getTemplateFor('displayWrapper') >> "displayWrapper"
        mockFormFieldsTemplateService.getTemplateFor('displayWidget') >> "displayWidget"
        mockFormFieldsTemplateService.getWidgetPrefix() >> 'input-'
		tagLib.formFieldsTemplateService = mockFormFieldsTemplateService
	}

    void 'tag body can be used instead of the input'() {
        given:
        views['/_fields/default/_wrapper.gsp'] = '${widget}'

        expect:
        applyTemplate('<f:field bean="personInstance" property="name">BODY</f:field>', [personInstance: personInstance]) == 'BODY'
    }

    void 'the model is passed to a tag body if there is one'() {
        given:
        views['/_fields/default/_wrapper.gsp'] = '${widget}'

        expect:
        applyTemplate('<f:field bean="personInstance" property="name">bean: ${bean.getClass().simpleName}, property: ${property}, type: ${type.simpleName}, label: ${label}, value: ${value}</f:field>', [personInstance: personInstance]) == 'bean: Person, property: name, type: String, label: Name, value: Bart Simpson'
    }

	@Issue("https://github.com/grails-fields-plugin/grails-fields/pull/49")
    void 'extra attributes prefixed with input- are passed to the tag body for backward compatibility'() {
        given:
        views['/_fields/default/_wrapper.gsp'] = '${widget}'

        expect:
        applyTemplate('<f:field bean="personInstance" property="name" input-foo="bar">${foo}</f:field>', [personInstance: personInstance]) == 'bar'
    }

    void 'extra attributes prefixed with input- are passed to the tag body grouped as "attrs"'() {
        given:
        views['/_fields/default/_wrapper.gsp'] = '${widget}'

        expect:
        applyTemplate('<f:field bean="personInstance" property="name" input-foo="bar">${attrs.foo}</f:field>', [personInstance: personInstance]) == 'bar'
    }
}