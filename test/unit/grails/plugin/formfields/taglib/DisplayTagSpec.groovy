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

        mockFormFieldsTemplateService.getTemplateFor('wrapper') >> "wrapper"
        mockFormFieldsTemplateService.getTemplateFor('widget') >> "widget"
        mockFormFieldsTemplateService.getTemplateFor('displayWrapper') >> "displayWrapper"
        mockFormFieldsTemplateService.getTemplateFor('displayWidget') >> "displayWidget"

		taglib.formFieldsTemplateService = mockFormFieldsTemplateService
        addRawMethodIfRequired tagLib
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
		views["/_fields/default/_displayWrapper.gsp"] = '<dt>${label}</dt><dd>${value}</dd>'

		and:
		mockFormFieldsTemplateService.findTemplate(_, 'displayWrapper', null) >> [path: '/_fields/default/displayWrapper']

		expect:
		applyTemplate('<f:display bean="personInstance" property="name"/>', [personInstance: personInstance]) == '<dt>Name</dt><dd>Bart Simpson</dd>'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/88')
	void 'display tag will use body for rendering value'() {
		given:
		views["/_fields/default/_displayWrapper.gsp"] = '<dt>${label}</dt><dd>${value}</dd>'

		and:
		mockFormFieldsTemplateService.findTemplate(_, 'displayWrapper', null) >> [path: '/_fields/default/displayWrapper']

		expect:
		applyTemplate('<f:display bean="personInstance" property="name">${value.reverse()}</f:display>', [personInstance: personInstance]) == '<dt>Name</dt><dd>nospmiS traB</dd>'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/192')
	void 'display tag will use body for rendering value2'() {
		given:
		views["/_fields/default/_displayWrapper.gsp"] = '<dt>${label}</dt><dd>${value}</dd>'

		and:
		mockFormFieldsTemplateService.findTemplate(_, 'displayWrapper', null) >> [path: '/_fields/default/displayWrapper']

		expect:
		applyTemplate('<f:display bean="personInstance" property="gender">${value.name()}</f:display>', [personInstance: personInstance]) == '<dt>Gender</dt><dd>Male</dd>'
	}

    @Issue('https://github.com/grails-fields-plugin/grails-fields/issues/135')
    void 'numeric properties are not converted to Strings in display template'() {
        given:
        views["/_fields/default/_displayWrapper.gsp"] = '<dt>${label}</dt><dd>${value}</dd>'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'displayWrapper', null) >> [path: '/_fields/default/displayWrapper']

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
        views["/_fields/default/_wrapper.gsp"] = '<dt>${label}</dt><dd>${widget}</dd>'
        views["/_fields/default/_widget.gsp"] = '${value.reverse()}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'wrapper', null) >> [path: '/_fields/default/wrapper']
        mockFormFieldsTemplateService.findTemplate(_, 'widget', null) >> [path: '/_fields/default/widget']

        expect:
        applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == '<dt>Name</dt><dd>nospmiS traB</dd>'
    }

    void 'render field template with the templates attribute'() {
        given:
        views["/_fields/widget/_wrapper.gsp"] = '<dt>WIDGET:</dt><dd>${widget}</dd>'
        views["/_fields/widget/_widget.gsp"] = '${value.reverse()}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'wrapper', "widget") >> [path: '/_fields/widget/wrapper']
        mockFormFieldsTemplateService.findTemplate(_, 'widget', "widget") >> [path: '/_fields/widget/widget']

        expect:
        applyTemplate('<f:field bean="personInstance" property="name" templates="widget"/>', [personInstance: personInstance]) == '<dt>WIDGET:</dt><dd>nospmiS traB</dd>'
    }

    void 'render field template with the field attribute'() {
        given:
        views["/_fields/widget/_wrapper.gsp"] = '<dt>WIDGET:</dt><dd>${widget}</dd>'
        views["/_fields/default/_widget.gsp"] = '${value}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'wrapper', "widget") >> [path: '/_fields/widget/wrapper']
        mockFormFieldsTemplateService.findTemplate(_, 'widget', null) >> [path: '/_fields/default/widget']

        expect:
        applyTemplate('<f:field bean="personInstance" property="name" wrapper="widget"/>', [personInstance: personInstance]) == '<dt>WIDGET:</dt><dd>Bart Simpson</dd>'
    }

    void 'render field template with the widget attribute'() {
        given:
        views["/_fields/default/_wrapper.gsp"] = '<dt>${label}</dt><dd>${widget}</dd>'
        views["/_fields/widget/_widget.gsp"] = '${value.reverse()}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'wrapper', null) >> [path: '/_fields/default/wrapper']
        mockFormFieldsTemplateService.findTemplate(_, 'widget', "widget") >> [path: '/_fields/widget/widget']

        expect:
        applyTemplate('<f:field bean="personInstance" property="name" widget="widget"/>', [personInstance: personInstance]) == '<dt>Name</dt><dd>nospmiS traB</dd>'
    }

    void 'render display template with the displayWidget inside of it'() {
        given:
        views["/_fields/default/_displayWrapper.gsp"] = '<dt>${label}</dt><dd>${widget}</dd>'
        views["/_fields/default/_displayWidget.gsp"] = '${value.reverse()}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'displayWrapper', null) >> [path: '/_fields/default/displayWrapper']
        mockFormFieldsTemplateService.findTemplate(_, 'displayWidget', null) >> [path: '/_fields/default/displayWidget']

        expect:
        applyTemplate('<f:display bean="personInstance" property="name"/>', [personInstance: personInstance]) == '<dt>Name</dt><dd>nospmiS traB</dd>'
    }

    void 'render display template with the templates attribute'() {
        given:
        views["/_fields/widget/_displayWrapper.gsp"] = '<dt>WIDGET:</dt><dd>${widget}</dd>'
        views["/_fields/widget/_displayWidget.gsp"] = '${value.reverse()}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'displayWrapper', "widget") >> [path: '/_fields/widget/displayWrapper']
        mockFormFieldsTemplateService.findTemplate(_, 'displayWidget', "widget") >> [path: '/_fields/widget/displayWidget']

        expect:
        applyTemplate('<f:display bean="personInstance" property="name" templates="widget"/>', [personInstance: personInstance]) == '<dt>WIDGET:</dt><dd>nospmiS traB</dd>'
    }

    void 'render display template with the field attribute'() {
        given:
        views["/_fields/widget/_displayWrapper.gsp"] = '<dt>WIDGET:</dt><dd>${widget}</dd>'
        views["/_fields/default/_displayWidget.gsp"] = '${value}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'displayWrapper', "widget") >> [path: '/_fields/widget/displayWrapper']
        mockFormFieldsTemplateService.findTemplate(_, 'displayWidget', null) >> [path: '/_fields/default/displayWidget']

        expect:
        applyTemplate('<f:display bean="personInstance" property="name" wrapper="widget"/>', [personInstance: personInstance]) == '<dt>WIDGET:</dt><dd>Bart Simpson</dd>'
    }

    void 'render display template with the widget attribute'() {
        given:
        views["/_fields/default/_displayWrapper.gsp"] = '<dt>${label}</dt><dd>${widget}</dd>'
        views["/_fields/widget/_displayWidget.gsp"] = '${value.reverse()}'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'displayWrapper', null) >> [path: '/_fields/default/displayWrapper']
        mockFormFieldsTemplateService.findTemplate(_, 'displayWidget', "widget") >> [path: '/_fields/widget/displayWidget']

        expect:
        applyTemplate('<f:display bean="personInstance" property="name" widget="widget"/>', [personInstance: personInstance]) == '<dt>Name</dt><dd>nospmiS traB</dd>'
    }

}
