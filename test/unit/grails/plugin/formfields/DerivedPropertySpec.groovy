package grails.plugin.formfields

import grails.plugin.formfields.mock.Product
import grails.plugin.formfields.taglib.AbstractFormFieldsTagLibSpec
import grails.test.mixin.*
import spock.lang.*

@Issue(['https://github.com/robfletcher/grails-fields/issues/85', 'https://github.com/robfletcher/grails-fields/issues/87'])
@TestFor(FormFieldsTagLib)
@Mock(Product)
@Unroll
class DerivedPropertySpec extends AbstractFormFieldsTagLibSpec {

    def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)
    Product productInstance

    def setupSpec() {
        configurePropertyAccessorSpringBean()
    }

    def setup() {
        def taglib = applicationContext.getBean(FormFieldsTagLib)

        mockFormFieldsTemplateService.findTemplate(_, 'field') >> [path: '/_fields/default/field']
        taglib.formFieldsTemplateService = mockFormFieldsTemplateService

        productInstance = new Product(name: 'MacBook Pro', netPrice: 1499, taxRate: 0.2).save(failOnError: true)
    }

    void '#reason properties are ignored by f:all'() {
        given:
        views["/_fields/default/_field.gsp"] = '${property} '

        when:
        def output = applyTemplate('<f:all bean="productInstance"/>', [productInstance: productInstance])

        then:
        !output.contains(property)

        where:
        property     | reason
        'tax'        | 'derived'
        'grossPrice' | 'transient'
    }

}
