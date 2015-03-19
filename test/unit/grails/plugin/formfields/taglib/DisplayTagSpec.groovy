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

    void 'render field template with the input inside of it'() {
        given:
        views["/_fields/default/_field.gsp"] = '<dt>${label}</dt><dd>${widget}</dd>'
        views["/_fields/default/_input.gsp"] = '${value.reverse()}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'field', null) >> [path: '/_fields/default/field']
        mockFormFieldsTemplateService.findTemplate(_, 'input', null) >> [path: '/_fields/default/input']

        expect:
        applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == '<dt>Name</dt><dd>nospmiS traB</dd>'
    }

    void 'render field template with the templates attribute'() {
        given:
        views["/_fields/widget/_field.gsp"] = '<dt>WIDGET:</dt><dd>${widget}</dd>'
        views["/_fields/widget/_input.gsp"] = '${value.reverse()}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'field', "widget") >> [path: '/_fields/widget/field']
        mockFormFieldsTemplateService.findTemplate(_, 'input', "widget") >> [path: '/_fields/widget/input']

        expect:
        applyTemplate('<f:field bean="personInstance" property="name" templates="widget"/>', [personInstance: personInstance]) == '<dt>WIDGET:</dt><dd>nospmiS traB</dd>'
    }

    void 'render field template with the field attribute'() {
        given:
        views["/_fields/widget/_field.gsp"] = '<dt>WIDGET:</dt><dd>${widget}</dd>'
        views["/_fields/default/_input.gsp"] = '${value}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'field', "widget") >> [path: '/_fields/widget/field']
        mockFormFieldsTemplateService.findTemplate(_, 'input', null) >> [path: '/_fields/default/input']

        expect:
        applyTemplate('<f:field bean="personInstance" property="name" field="widget"/>', [personInstance: personInstance]) == '<dt>WIDGET:</dt><dd>Bart Simpson</dd>'
    }

    void 'render field template with the widget attribute'() {
        given:
        views["/_fields/default/_field.gsp"] = '<dt>${label}</dt><dd>${widget}</dd>'
        views["/_fields/widget/_input.gsp"] = '${value.reverse()}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'field', null) >> [path: '/_fields/default/field']
        mockFormFieldsTemplateService.findTemplate(_, 'input', "widget") >> [path: '/_fields/widget/input']

        expect:
        applyTemplate('<f:field bean="personInstance" property="name" widget="widget"/>', [personInstance: personInstance]) == '<dt>Name</dt><dd>nospmiS traB</dd>'
    }

    void 'render display template with the displayOutput inside of it'() {
        given:
        views["/_fields/default/_display.gsp"] = '<dt>${label}</dt><dd>${widget}</dd>'
        views["/_fields/default/_displayOutput.gsp"] = '${value.reverse()}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'display', null) >> [path: '/_fields/default/display']
        mockFormFieldsTemplateService.findTemplate(_, 'displayOutput', null) >> [path: '/_fields/default/displayOutput']

        expect:
        applyTemplate('<f:display bean="personInstance" property="name"/>', [personInstance: personInstance]) == '<dt>Name</dt><dd>nospmiS traB</dd>'
    }

    void 'render display template with the templates attribute'() {
        given:
        views["/_fields/widget/_display.gsp"] = '<dt>WIDGET:</dt><dd>${widget}</dd>'
        views["/_fields/widget/_displayOutput.gsp"] = '${value.reverse()}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'display', "widget") >> [path: '/_fields/widget/display']
        mockFormFieldsTemplateService.findTemplate(_, 'displayOutput', "widget") >> [path: '/_fields/widget/displayOutput']

        expect:
        applyTemplate('<f:display bean="personInstance" property="name" templates="widget"/>', [personInstance: personInstance]) == '<dt>WIDGET:</dt><dd>nospmiS traB</dd>'
    }

    void 'render display template with the field attribute'() {
        given:
        views["/_fields/widget/_display.gsp"] = '<dt>WIDGET:</dt><dd>${widget}</dd>'
        views["/_fields/default/_displayOutput.gsp"] = '${value}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'display', "widget") >> [path: '/_fields/widget/display']
        mockFormFieldsTemplateService.findTemplate(_, 'displayOutput', null) >> [path: '/_fields/default/displayOutput']

        expect:
        applyTemplate('<f:display bean="personInstance" property="name" field="widget"/>', [personInstance: personInstance]) == '<dt>WIDGET:</dt><dd>Bart Simpson</dd>'
    }

    void 'render display template with the widget attribute'() {
        given:
        views["/_fields/default/_display.gsp"] = '<dt>${label}</dt><dd>${widget}</dd>'
        views["/_fields/widget/_displayOutput.gsp"] = '${value.reverse()}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'display', null) >> [path: '/_fields/default/display']
        mockFormFieldsTemplateService.findTemplate(_, 'displayOutput', "widget") >> [path: '/_fields/widget/displayOutput']

        expect:
        applyTemplate('<f:display bean="personInstance" property="name" widget="widget"/>', [personInstance: personInstance]) == '<dt>Name</dt><dd>nospmiS traB</dd>'
    }

}
