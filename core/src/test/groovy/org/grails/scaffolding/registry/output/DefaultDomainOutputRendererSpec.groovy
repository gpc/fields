package org.grails.scaffolding.registry.output

import org.grails.scaffolding.ClosureCapture
import org.grails.scaffolding.ClosureCaptureSpecification
import org.grails.scaffolding.model.property.DomainProperty
import spock.lang.Shared
import spock.lang.Subject

/**
 * Created by Jim on 6/7/2016.
 */
@Subject(DefaultOutputRenderer)
class DefaultDomainOutputRendererSpec extends ClosureCaptureSpecification {

    @Shared
    DefaultOutputRenderer renderer

    void setup() {
        renderer = new DefaultOutputRenderer()
    }

    void "test render"() {
        given:
        DomainProperty property

        when:
        property = Mock(DomainProperty) {
            1 * getRootBeanType() >> Calendar
            1 * getPathFromRoot() >> "time"
        }
        ClosureCapture closureCapture = getClosureCapture(renderer.renderOutput(property))

        then:
        closureCapture.calls[0].name == "span"
        closureCapture.calls[0].args[0] == "\${calendar.time}"
    }

    void "test render list"() {
        given:
        DomainProperty property

        when:
        property = Mock(DomainProperty) {
            1 * getRootBeanType() >> Calendar
            1 * getPathFromRoot() >> "time"
        }
        ClosureCapture closureCapture = getClosureCapture(renderer.renderOutput(property))

        then:
        closureCapture.calls[0].name == "span"
        closureCapture.calls[0].args[0] == "\${calendar.time}"
    }
}
