package grails.plugin.formfields.taglib

import grails.plugin.formfields.FormFieldsTagLib
import grails.plugin.formfields.FormFieldsTemplateService
import grails.plugin.formfields.mock.Author
import grails.plugin.formfields.mock.Book
import grails.plugin.formfields.mock.Person
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Issue

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

    void 'scoped bean stack is pushed/popped when f:with tag is opened/closed'() {
        expect:
        applyTemplate('<f:with bean="personInstance">${request.getAttribute("grails.plugins.formfields.WITH_STACK").size()}</f:with>:${request.getAttribute("grails.plugins.formfields.WITH_STACK").size()}', [personInstance: personInstance]) == '1:0'
    }

    void 'scoped beans can be nested'() {
        given:
        views['/_fields/default/_wrapper.gsp'] = '${value} '

        expect:
        applyTemplate('<f:with bean="productInstance"><f:field property="netPrice"/><f:with bean="personInstance"><f:field property="name"/></f:with></f:with>', [personInstance: personInstance, productInstance: productInstance]) == '12.33 Bart Simpson '
    }

    @Issue('https://github.com/grails-fields-plugin/grails-fields/issues/206')
    void 'scoped beans can be nested within templates'() {
        given:
        Author authorInstance = new Author(name: "Albert Camus")
        Book bookInstance = new Book(title: "Les Muets", author: authorInstance)
        authorInstance.books = [bookInstance]
        views['/_fields/author/books/_displayWidget.gsp'] = '<g:each in="${value}" var="book"><f:with bean="${book}"><f:display property="title"/></f:with></g:each>'
        views['/_fields/book/title/_displayWidget.gsp'] = '${value}'
        mockFormFieldsTemplateService.findTemplate(_, 'displayWidget', null) >>> [
                [path: '/_fields/author/books/displayWidget'],
                [path: '/_fields/book/title/displayWidget']
        ]

        expect:
        applyTemplate('<f:with bean="authorInstance"><f:display property="books"/> <f:display property="name"/></f:with>', [authorInstance: authorInstance]) == 'Les Muets Albert Camus'
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
