package grails.plugin.formfields

import grails.plugin.formfields.mock.Product
import grails.plugin.formfields.taglib.AbstractFormFieldsTagLibSpec
import spock.lang.Issue
import grails.test.mixin.*
import grails.plugin.formfields.mock.User

@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/87')
@TestFor(FormFieldsTagLib)
@Mock(User)
class TransientPropertySpec extends AbstractFormFieldsTagLibSpec {

    def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)
    User userInstance

    def setupSpec() {
        configurePropertyAccessorSpringBean()
    }

    def setup() {
        def taglib = applicationContext.getBean(FormFieldsTagLib)

        views["/_fields/_layouts/_noLayout.gsp"] = '${raw(renderedField)}'
        mockFormFieldsTemplateService.findTemplate(_, 'field', null) >> [path: '/_fields/default/field']
        taglib.formFieldsTemplateService = mockFormFieldsTemplateService

        userInstance = new User(email: 'rob@freeside.co', password: 'yuonocanhaz', confirmPassword: 'yuonocanhaz').save(failOnError: true)
    }

    void 'transient properties can be rendered by f:field'() {
        given:
        views["/_fields/default/_field.gsp"] = '${value}'

        when:
        def output = applyTemplate('<f:field bean="userInstance" property="confirmPassword"/>', [userInstance: userInstance])

        then:
        output == userInstance.confirmPassword
    }
}
