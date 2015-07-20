package grails.plugin.formfields

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.util.Metadata
import org.codehaus.groovy.grails.plugins.codecs.HTMLCodec
import spock.lang.Specification
import spock.lang.Unroll

@TestMixin(GrailsUnitTestMixin)
class LegacyGrailsSupportSpec extends Specification {
    final String actualGrailsVersion = Metadata.current.getGrailsVersion()

    void setup() {
        mockCodec(HTMLCodec)
    }

    void cleanup() {
        grailsVersion = actualGrailsVersion
    }

    @Unroll('returns #expected when Grails version is #version')
    void 'returns true if Grails is a supported legacy version'() {
        given:
        grailsVersion = version

        expect:
        LegacyGrailsSupport.supportedLegacyGrailsVersion == expected

        where:
        version | expected
        '1.3.9' | false
        '2.0.0' | true
        '2.1.0' | true
        '2.2.0' | true
        '2.2.5' | true
        '2.3.0' | false
        '2.4.0' | false
        '2.5.0' | false
        '3.0.0' | false
        '3.0.3' | false
    }

    void 'add raw() method to artefact'() {
        given:
        def dummyArtefact = new Object()

        when:
        dummyArtefact.raw('test string')

        then:
        thrown MissingMethodException

        when:
        LegacyGrailsSupport.addRawMethodImplementation(dummyArtefact)

        and:
        dummyArtefact.raw('test string')

        then:
        notThrown MissingMethodException
    }

    void 'raw() method returns an equivalent String'() {
        given:
        def dummyArtefact = new Object()
        LegacyGrailsSupport.addRawMethodImplementation(dummyArtefact)

        expect:
        dummyArtefact.raw('test string') == 'test string'
    }

    void 'raw() method returns a new copy a String instance'() {
        given:
        def dummyArtefact = new Object()
        LegacyGrailsSupport.addRawMethodImplementation(dummyArtefact)

        and:
        def value = 'test string'

        when:
        def result = dummyArtefact.raw(value)

        then:
        !result.is(value)
    }

    void 'raw() returns a String when passed an non-String CharSequence'() {
        given:
        def dummyArtefact = new Object()
        LegacyGrailsSupport.addRawMethodImplementation(dummyArtefact)

        and:
        def value = 'Char' << 'Sequence'

        expect:
        value instanceof CharSequence && !(value instanceof String)

        when:
        def result = dummyArtefact.raw(value)

        then:
        result instanceof String
        result == 'CharSequence'
    }

    void 'raw() method returns non-CharSequence instance untouched'() {
        given:
        def dummyArtefact = new Object()
        LegacyGrailsSupport.addRawMethodImplementation(dummyArtefact)

        and:
        def value = new Date()

        when:
        def result = dummyArtefact.raw(value)

        then:
        result.is value
    }

    private void setGrailsVersion(String version) {
        Metadata.current.put(Metadata.APPLICATION_GRAILS_VERSION, version)
    }
}
