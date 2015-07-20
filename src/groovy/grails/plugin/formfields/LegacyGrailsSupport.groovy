package grails.plugin.formfields

import grails.util.Metadata

class LegacyGrailsSupport  {
    static boolean isSupportedLegacyGrailsVersion() {
        Metadata.current.getGrailsVersion() ==~ /2\.[0-2]\.\d*/
    }

    static addRawMethodImplementation(Object artefact) {
        artefact.metaClass.raw = { Object value ->
            if (value instanceof CharSequence) {
                new String(value.toString())
            }
            else {
                value
            }
        }
    }
}
