package grails.plugin.formfields

import grails.plugin.formfields.mock.Product
import grails.plugin.formfields.taglib.AbstractFormFieldsTagLibSpec
import grails.test.mixin.*
import spock.lang.*

@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/85')
@TestFor(FormFieldsTagLib)
@Mock(Product)
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

        // @Mock isn't aware of formulae so we need to set this manually
        grailsApplication.getDomainClass(Product.name).getPersistentProperty('tax').derived = true

        productInstance = new Product(name: 'MacBook Pro', netPrice: 1499, taxRate: 0.2).save(failOnError: true)
    }

    void 'derived properties are ignored by f:all'() {
        given:
        views["/_fields/default/_wrapper.gsp"] = '${property} '

        when:
        def output = applyTemplate('<f:all bean="productInstance"/>', [productInstance: productInstance])

        then:
        !(output =~ /\btax\b/)
    }

}
