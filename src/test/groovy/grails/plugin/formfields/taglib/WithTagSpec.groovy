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

		mockFormFieldsTemplateService.findTemplate(_, 'wrapper', null) >> [path: '/_fields/default/wrapper']
        mockFormFieldsTemplateService.getTemplateFor('wrapper') >> "wrapper"
        mockFormFieldsTemplateService.getTemplateFor('widget') >> "widget"
        mockFormFieldsTemplateService.getTemplateFor('displayWrapper') >> "displayWrapper"
        mockFormFieldsTemplateService.getTemplateFor('displayWidget') >> "displayWidget"
        mockFormFieldsTemplateService.getWidgetPrefix() >> 'input-'
		taglib.formFieldsTemplateService = mockFormFieldsTemplateService

        mockEmbeddedSitemeshLayout taglib
    }

	void 'bean attribute does not have to be specified if it is in scope from f:with'() {
		given:
		views["/_fields/default/_wrapper.gsp"] = '${property} '

		expect:
		applyTemplate('<f:with bean="personInstance"><f:field property="name"/></f:with>', [personInstance: personInstance]) == 'name '
	}

	void 'scoped bean attribute does not linger around after f:with tag'() {
		expect:
		applyTemplate('<f:with bean="personInstance">${pageScope.getVariable("f:with:stack")}</f:with>${pageScope.getVariable("f:with:stack")}', [personInstance: personInstance]) == 'Bart Simpson'
	}

    void 'scoped beans can be nested'() {
        given:
        views['/_fields/default/_wrapper.gsp'] = '${value} '

        expect:
        applyTemplate('<f:with bean="productInstance"><f:field property="netPrice"/><f:with bean="personInstance"><f:field property="name"/></f:with></f:with>', [personInstance: personInstance, productInstance: productInstance]) == '12.33 Bart Simpson '
    }

    void 'embedded attributes work if in scope from f:with'() {
        given:
        views['/_fields/default/_wrapper.gsp'] = '${property} '

        when:
        def output = applyTemplate('<f:with bean="personInstance"><f:field property="address"/></f:with>', [personInstance: personInstance])

        then:
        output.contains('address.street address.city address.country')
    }

}