package grails.plugin.formfields.taglib

import grails.plugin.formfields.mock.Person
import grails.plugin.formfields.*
import grails.test.mixin.*
import org.grails.taglib.GrailsTagException
import spock.lang.*

@TestFor(FormFieldsTagLib)
@Mock(Person)
@Unroll
class AllTagSpec extends AbstractFormFieldsTagLibSpec {

    def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

    def setupSpec() {
        configurePropertyAccessorSpringBean()
    }

    def setup() {
        def taglib = applicationContext.getBean(FormFieldsTagLib)

        mockFormFieldsTemplateService.getTemplateFor(_) >> { args -> args[0]}
        mockFormFieldsTemplateService.findTemplate(_, 'widget', _) >> [path: '/_fields/default/field']
        mockFormFieldsTemplateService.findTemplate(_, 'wrapper', _) >> [path: '/_fields/default/wrapper']
        taglib.formFieldsTemplateService = mockFormFieldsTemplateService

        mockEmbeddedSitemeshLayout(taglib)
    }

    void "all tag renders fields for all properties"() {
        given:
        views["/_fields/default/_field.gsp"] = '${property} '
        views["/_fields/default/_wrapper.gsp"] = '${widget}'

        when:
        def output = applyTemplate('<f:all bean="personInstance"/>', [personInstance: personInstance])

        then:
        output =~ /\bname\b/
        output =~ /\bpassword\b/
        output =~ /\bgender\b/
        output =~ /\bdateOfBirth\b/
        output =~ /\bminor\b/
    }

    @Issue('https://github.com/grails-fields-plugin/grails-fields/issues/21')
    void 'all tag skips #excluded property and includes #included property'() {
        given:
        views["/_fields/default/_field.gsp"] = '${property} '
        views["/_fields/default/_wrapper.gsp"] = '${widget}'

        when:
        def output = applyTemplate('<f:all bean="personInstance"/>', [personInstance: personInstance])

        then:
        !output.contains(excluded)
        output.contains(included)

        where:
        excluded << ['id', 'version', 'onLoad', 'lastUpdated', 'excludedProperty', 'displayFalseProperty']
        included << ['salutation', 'name', 'password', 'gender', 'dateOfBirth', 'address.street']
    }

    @Issue('https://github.com/grails-fields-plugin/grails-fields/issues/12')
    void 'all tag skips properties listed with the except attribute'() {
        given:
        views["/_fields/default/_field.gsp"] = '${property} '
        views["/_fields/default/_wrapper.gsp"] = '${widget}'

        when:
        def output = applyTemplate('<f:all bean="personInstance" except="password, minor"/>', [personInstance: personInstance])

        then:
        !output.contains('password')
        !output.contains('minor')
    }

    @Issue('https://github.com/grails3-plugins/fields/issues/9')
    void 'all tag respects the order attribute'() {
        given:
        views["/_fields/default/_field.gsp"] = '|${property}|'
        views["/_fields/default/_wrapper.gsp"] = '${widget}'

        when:
        def output = applyTemplate('<f:all bean="personInstance" order="name, minor, gender"/>', [personInstance: personInstance])

        then:
        output == '|name||minor||gender|'

    }

    @Issue('https://github.com/grails3-plugins/fields/issues/9')
    void 'order attribute and except attribute are mutually exclusive'() {
        given:
        views["/_fields/default/_field.gsp"] = '|${property}|'
        views["/_fields/default/_wrapper.gsp"] = '${widget}'

        when:
        applyTemplate('<f:all bean="personInstance" except="password" order="name, minor, gender"/>', [personInstance: personInstance])

        then:
        GrailsTagException e = thrown()
        e.message.contains 'The [except] and [order] attributes may not be used together.'
    }
}
