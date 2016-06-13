package org.grails.scaffolding.markup

import org.grails.scaffolding.model.DomainModelService
import org.grails.scaffolding.model.property.DomainProperty
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Embedded
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by Jim on 5/29/2016.
 */
class DomainMarkupRendererSpec extends Specification {

    @Shared
    DomainMarkupRendererImpl renderer

    void setup() {
        renderer = new DomainMarkupRendererImpl()
    }

    void "test renderListOutput"() {
        given:
        DomainProperty prop1 = Mock(DomainProperty) {
            1 * getName() >> "prop1"
        }
        DomainProperty prop2 = Mock(DomainProperty) {
            1 * getName() >> "prop2"
        }
        DomainProperty prop3 = Mock(DomainProperty) {
            1 * getName() >> "prop3"
        }
        List props = [prop1, prop2, prop3]
        renderer.domainModelService = Mock(DomainModelService) {
            1 * getListOutputProperties(_ as PersistentEntity) >> props
        }
        renderer.contextMarkupRenderer = Mock(ContextMarkupRenderer) {
            1 * listOutputContext(_ as PersistentEntity, props, _ as Closure) >> { entity, properties, closure ->
                return { ->
                    properties.each { DomainProperty prop ->
                        div(closure.call(prop))
                    }
                }
            }
        }
        renderer.propertyMarkupRenderer = Mock(PropertyMarkupRenderer) {
            3 * renderListOutput(_ as DomainProperty) >> { DomainProperty prop ->
                return { -> span(prop.name) }
            }
        }

        when:
        String output = renderer.renderListOutput(Mock(PersistentEntity))

        then:
        output == "<div>\n  <span>prop1</span>\n</div>\n<div>\n  <span>prop2</span>\n</div>\n<div>\n  <span>prop3</span>\n</div>"
    }

    void "test renderForm"() {
        given:
        PersistentEntity domain = Mock(PersistentEntity)
        PersistentEntity embedded = Mock(PersistentEntity)
        DomainProperty prop1 = Mock(DomainProperty) {
            1 * getName() >> "prop1"
            1 * getPersistentProperty() >> Mock(PersistentProperty)
        }
        DomainProperty prop2 = Mock(DomainProperty) {
            2 * getPersistentProperty() >> Mock(Embedded) {
                1 * getAssociatedEntity() >> embedded
            }
        }
        DomainProperty prop3 = Mock(DomainProperty) {
            1 * getName() >> "prop3"
        }
        renderer.domainModelService = Mock(DomainModelService) {
            1 * getInputProperties(domain) >> [prop1, prop2]
            1 * getInputProperties(embedded) >> [prop3]
        }
        renderer.contextMarkupRenderer = Mock(ContextMarkupRenderer) {
            2 * inputContext(_ as DomainProperty, _ as Closure) >> { DomainProperty prop, Closure c ->
                return { ->
                    div(c)
                }
            }
            1 * inputContext(_ as PersistentEntity, _ as Closure) >> { PersistentEntity d, Closure c ->
                return { ->
                    form(c)
                }
            }
            1 * embeddedInputContext(_ as DomainProperty, _ as Closure) >> { DomainProperty prop, Closure c ->
                return { ->
                    fieldset(c)
                }
            }
        }
        renderer.propertyMarkupRenderer = Mock(PropertyMarkupRenderer) {
            2 * renderInput(_ as DomainProperty) >> { DomainProperty prop ->
                return { -> span(prop.name) }
            }
        }

        when:
        String output = renderer.renderInput(domain)

        then:
        output == "<form>\n  <div>\n    <span>prop1</span>\n  </div>\n  <fieldset>\n    <div>\n      <span>prop3</span>\n    </div>\n  </fieldset>\n</form>"
    }

    void "test renderOutput"() {
        given:
        PersistentEntity domain = Mock(PersistentEntity)
        PersistentEntity embedded = Mock(PersistentEntity)
        DomainProperty prop1 = Mock(DomainProperty) {
            1 * getName() >> "prop1"
            1 * getPersistentProperty() >> Mock(PersistentProperty)
        }
        DomainProperty prop2 = Mock(DomainProperty) {
            2 * getPersistentProperty() >> Mock(Embedded) {
                1 * getAssociatedEntity() >> embedded
            }
        }
        DomainProperty prop3 = Mock(DomainProperty) {
            1 * getName() >> "prop3"
        }
        renderer.domainModelService = Mock(DomainModelService) {
            1 * getOutputProperties(domain) >> [prop1, prop2]
            1 * getOutputProperties(embedded) >> [prop3]
        }
        renderer.contextMarkupRenderer = Mock(ContextMarkupRenderer) {
            2 * outputContext(_ as DomainProperty, _ as Closure) >> { DomainProperty prop, Closure c ->
                return { ->
                    div(c)
                }
            }
            1 * outputContext(_ as PersistentEntity, _ as Closure) >> { PersistentEntity d, Closure c ->
                return { ->
                    form(c)
                }
            }
            1 * embeddedOutputContext(_ as DomainProperty, _ as Closure) >> { DomainProperty prop, Closure c ->
                return { ->
                    fieldset(c)
                }
            }
        }
        renderer.propertyMarkupRenderer = Mock(PropertyMarkupRenderer) {
            2 * renderOutput(_ as DomainProperty) >> { DomainProperty prop ->
                return { -> span(prop.name) }
            }
        }

        when:
        String output = renderer.renderOutput(domain)

        then:
        output == "<form>\n  <div>\n    <span>prop1</span>\n  </div>\n  <fieldset>\n    <div>\n      <span>prop3</span>\n    </div>\n  </fieldset>\n</form>"
    }
}
