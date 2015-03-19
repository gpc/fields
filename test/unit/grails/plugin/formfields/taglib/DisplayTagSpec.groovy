package grails.plugin.formfields.taglib

import grails.test.mixin.TestFor
import spock.lang.Issue
import grails.plugin.formfields.*

@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/45')
@TestFor(FormFieldsTagLib)
class DisplayTagSpec extends AbstractFormFieldsTagLibSpec {

	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

	def setupSpec() {
		configurePropertyAccessorSpringBean()
	}

	def setup() {
		def taglib = applicationContext.getBean(FormFieldsTagLib)
		views["/_fields/_layouts/_noLayout.gsp"] = '${raw(renderedField)}'
		mockFormFieldsTemplateService.findTemplate(_, 'field', null) >> null
		taglib.formFieldsTemplateService = mockFormFieldsTemplateService
	}

	void 'renders value using g:fieldValue if no template is present'() {
		expect:
		applyTemplate('<f:display bean="personInstance" property="name"/>', [personInstance: personInstance]) == personInstance.name
	}

	void 'renders boolean values using g:formatBoolean'() {
		given:
		messageSource.addMessage('default.boolean.true', request.locale, 'Yes')

		expect:
		applyTemplate('<f:display bean="personInstance" property="minor"/>', [personInstance: personInstance]) == 'Yes'
	}

	void 'renders date values using g:formatDate'() {
		expect:
		applyTemplate('<f:display bean="personInstance" property="dateOfBirth"/>', [personInstance: personInstance]) ==~ /1987-04-19 00:00:00 [A-Z]{3,4}/
	}

	void 'displays using template if one is present'() {
		given:
		views["/_fields/default/_display.gsp"] = '<dt>${label}</dt><dd>${value}</dd>'

		and:
		mockFormFieldsTemplateService.findTemplate(_, 'display', null) >> [path: '/_fields/default/display']

		expect:
		applyTemplate('<f:display bean="personInstance" property="name"/>', [personInstance: personInstance]) == '<dt>Name</dt><dd>Bart Simpson</dd>'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/88')
	void 'display tag will use body for rendering value'() {
		given:
		views["/_fields/default/_display.gsp"] = '<dt>${label}</dt><dd>${value}</dd>'

		and:
		mockFormFieldsTemplateService.findTemplate(_, 'display', null) >> [path: '/_fields/default/display']

		expect:
		applyTemplate('<f:display bean="personInstance" property="name">${value.reverse()}</f:display>', [personInstance: personInstance]) == '<dt>Name</dt><dd>nospmiS traB</dd>'
	}

    @Issue('https://github.com/grails-fields-plugin/grails-fields/issues/135')
    void 'numeric properties are not converted to Strings in display template'() {
        given:
        views["/_fields/default/_display.gsp"] = '<dt>${label}</dt><dd>${value}</dd>'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'display', null) >> [path: '/_fields/default/display']

        expect:
        def expectedDisplayedPrice = productInstance.netPrice.round(1)
        applyTemplate('<f:display bean="productInstance" property="netPrice">${g.formatNumber(number: value, maxFractionDigits: 1)}</f:display>',
                [productInstance: productInstance]) == "<dt>Net Price</dt><dd>$expectedDisplayedPrice</dd>"
    }

    void 'can nest f:display inside f:with'() {
        expect:
        applyTemplate('<f:with bean="personInstance"><f:display property="name"/></f:with>', [personInstance: personInstance]) == personInstance.name
    }

    @Issue('https://github.com/grails-fields-plugin/grails-fields/issues/160')
    void 'renders transients using g:fieldValue'() {
        expect:
        applyTemplate('<f:display bean="personInstance" property="transientText"/>', [personInstance: personInstance]) == personInstance.transientText
    }


}
