package grails.plugin.formfields.taglib

import grails.plugin.formfields.FormFieldsTagLib
import grails.plugin.formfields.FormFieldsTemplateService
import grails.plugin.formfields.mock.Author
import grails.plugin.formfields.mock.Book
import grails.plugin.formfields.mock.Person
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Issue

@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/45')
@TestFor(FormFieldsTagLib)
@Mock([Person, Author, Book])
class DisplayTagSpec extends AbstractFormFieldsTagLibSpec {

	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

	def setupSpec() {
		configurePropertyAccessorSpringBean()
	}

	def setup() {
		def taglib = applicationContext.getBean(FormFieldsTagLib)

		taglib.formFieldsTemplateService = mockFormFieldsTemplateService
	}

	void 'renders all properties as list'() {
		when:"A list is rendered"
			def result = applyTemplate('<f:display bean="personInstance" />', [personInstance: personInstance])

		then:"The result is a list"
			result.contains '<ol class="property-list person">'
			result.contains '<span id="gender-label" class="property-label">Gender</span>'
			result.contains '<div class="property-value" aria-labelledby="gender-label">Male</div>'
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
		mockFormFieldsTemplateService.findTemplate(_, 'display') >> [path: '/_fields/default/display']

		expect:
		applyTemplate('<f:display bean="personInstance" property="name"/>', [personInstance: personInstance]) == '<dt>Name</dt><dd>Bart Simpson</dd>'
	}

	void 'displayStyle attribute allows to use a specific template'() {
		given:
		views["/_fields/default/_display.gsp"] = '<dt>${label}</dt><dd>${value}</dd>'
		views["/_fields/default/_display-custom.gsp"] = 'Custom: ${value}'

		and:
		mockFormFieldsTemplateService.findTemplate(_, 'display') >> [path: '/_fields/default/display']
		mockFormFieldsTemplateService.findTemplate(_, 'display-custom') >> [path: '/_fields/default/display-custom']

		expect: "'default' displayStyle uses 'display' template"
		applyTemplate('<f:display bean="personInstance" property="name" displayStyle="default"/>', [personInstance: personInstance]) == '<dt>Name</dt><dd>Bart Simpson</dd>'

		and: "'custom' displayStyle uses 'display-custom' template"
		applyTemplate('<f:display bean="personInstance" property="name" displayStyle="custom"/>', [personInstance: personInstance]) == 'Custom: Bart Simpson'
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/88')
	void 'display tag will use body for rendering value'() {
		given:
		views["/_fields/default/_display.gsp"] = '<dt>${label}</dt><dd>${value}</dd>'

		and:
		mockFormFieldsTemplateService.findTemplate(_, 'display') >> [path: '/_fields/default/display']

		expect:
		applyTemplate('<f:display bean="personInstance" property="name">${value.reverse()}</f:display>', [personInstance: personInstance]) == '<dt>Name</dt><dd>nospmiS traB</dd>'
	}

    @Issue('https://github.com/grails-fields-plugin/grails-fields/issues/135')
    void 'numeric properties are not converted to Strings in display template'() {
        given:
        views["/_fields/default/_display.gsp"] = '<dt>${label}</dt><dd>${value}</dd>'

        and:
        mockFormFieldsTemplateService.findTemplate(_, 'display') >> [path: '/_fields/default/display']

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

	void 'renders all embedded components properties'() {
		when: "display an embedded address"
		def result= applyTemplate('<f:display bean="personInstance" property="address"/>', [personInstance: personInstance])

		then: "the result contains all embedded address properties"
		result.contains('<ol class="property-list address">')
		result.contains('<span id="street-label" class="property-label">Street</span>')
		result.contains('<div class="property-value" aria-labelledby="street-label">94 Evergreen Terrace</div>')
		result.contains('<div class="property-value" aria-labelledby="city-label">Springfield</div>')
		result.contains('<div class="property-value" aria-labelledby="country-label">USA</div>')
	}

	void 'renders many-side associations as a list of links'() {
		given:
		def book1 = new Book(title: 'book 1')
		def book2 = new Book(title: 'book 2')
		def author = new Author().addToBooks(book1).addToBooks(book2)

		when:
		def result = applyTemplate('<f:display bean="author"/>', [author: author])

		then:
		result.contains('<div class="property-value" aria-labelledby="books-label"><ul><li><a href="/book/show">book 1</a></li><li><a href="/book/show">book 2</a></li></ul></div>')
	}

	void 'renders one-side associations as a link'() {
		given:
		def book=new Book(title: 'the title')
		new Author(name: 'Bart Simpson').addToBooks(book)

		when:
		def result = applyTemplate('<f:display bean="book"/>', [book: book])

		then:
		result.contains('<div class="property-value" aria-labelledby="author-label"><a href="/author/show">Bart Simpson</a></div>')
	}


}
