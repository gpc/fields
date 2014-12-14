package grails.plugin.formfields.taglib

import grails.plugin.formfields.mock.Person
import spock.lang.Issue
import grails.plugin.formfields.*
import grails.test.mixin.*

@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/13')
@TestFor(FormFieldsTagLib)
@Mock(Person)
class WithTagSpec extends AbstractFormFieldsTagLibSpec {

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

        mockEmbeddedSitemeshLayout taglib
    }

	void 'bean attribute does not have to be specified if it is in scope from f:with'() {
		given:
		views["/_fields/default/_field.gsp"] = '${property} '

		expect:
		applyTemplate('<f:with bean="personInstance"><f:field property="name"/></f:with>', [personInstance: personInstance]) == 'name '
	}

	void 'scoped bean attribute does not linger around after f:with tag'() {
		expect:
		applyTemplate('<f:with bean="personInstance">${pageScope.getVariable("f:with:bean")}</f:with>${pageScope.getVariable("f:with:bean")}', [personInstance: personInstance]) == 'Bart Simpson'
	}

    void 'embedded attributes work if in scope from f:with'() {
        given:
        views['/_fields/default/_field.gsp'] = '${property} '

        when:
        def output = applyTemplate('<f:with bean="personInstance"><f:field property="address"/></f:with>', [personInstance: personInstance])

        then:
        output.contains('address.street address.city address.country')
    }

}