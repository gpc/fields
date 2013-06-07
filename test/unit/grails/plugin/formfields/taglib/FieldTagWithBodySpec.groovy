package grails.plugin.formfields.taglib

import grails.plugin.formfields.mock.Person
import spock.lang.Issue
import grails.plugin.formfields.*
import grails.test.mixin.*

@Issue('https://github.com/robfletcher/grails-fields/pull/16')
@TestFor(FormFieldsTagLib)
@Mock(Person)
class FieldTagWithBodySpec extends AbstractFormFieldsTagLibSpec {

	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

	def setupSpec() {
		configurePropertyAccessorSpringBean()
	}

	def setup() {
		def taglib = applicationContext.getBean(FormFieldsTagLib)

		mockFormFieldsTemplateService.findTemplate(_, 'field', "") >> [path: '/_fields/default/field']
		taglib.formFieldsTemplateService = mockFormFieldsTemplateService
	}

    void 'tag body can be used instead of the input'() {
        given:
        views['/_fields/default/_field.gsp'] = '${widget}'

        expect:
        applyTemplate('<f:field bean="personInstance" property="name">BODY</f:field>', [personInstance: personInstance]) == 'BODY'
    }

    void 'the model is passed to a tag body if there is one'() {
        given:
        views['/_fields/default/_field.gsp'] = '${widget}'

        expect:
        applyTemplate('<f:field bean="personInstance" property="name">bean: ${bean.getClass().simpleName}, property: ${property}, type: ${type.simpleName}, label: ${label}, value: ${value}</f:field>', [personInstance: personInstance]) == 'bean: Person, property: name, type: String, label: Name, value: Bart Simpson'
    }

	@Issue("https://github.com/robfletcher/grails-fields/pull/49")
    void 'extra attributes prefixed with input- are passed to the tag body'() {
        given:
        views['/_fields/default/_field.gsp'] = '${widget}'

        expect:
        applyTemplate('<f:field bean="personInstance" property="name" input-foo="bar">${foo}</f:field>', [personInstance: personInstance]) == 'bar'
    }

}