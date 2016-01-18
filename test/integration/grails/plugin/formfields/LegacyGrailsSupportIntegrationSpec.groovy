package grails.plugin.formfields

import grails.util.Holders
import spock.lang.IgnoreIf
import spock.lang.Issue
import spock.lang.Specification

@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/197')
@IgnoreIf({ !LegacyGrailsSupport.supportedLegacyGrailsVersion })
class LegacyGrailsSupportIntegrationSpec extends Specification {

    def grailsApplication

    void setup() {
        grailsApplication = Holders.grailsApplication
    }

    void 'raw() method is added to FormFieldsTagLib'() {
        given:
        def tagLib = grailsApplication.mainContext.getBean(FormFieldsTagLib)

        when:
        tagLib.raw('test value')

        then:
        notThrown MissingMethodException
    }

}
