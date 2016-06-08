package org.grails.scaffolding.registry

import org.grails.scaffolding.model.property.DomainProperty
import org.grails.scaffolding.registry.input.AssociationInputRenderer
import org.grails.scaffolding.registry.input.BidirectionalToManyInputRenderer
import org.grails.scaffolding.registry.input.BooleanInputRenderer
import org.grails.scaffolding.registry.input.CurrencyInputRenderer
import org.grails.scaffolding.registry.input.DateInputRenderer
import org.grails.scaffolding.registry.input.DefaultInputRenderer
import org.grails.scaffolding.registry.input.EnumInputRenderer
import org.grails.scaffolding.registry.input.FileInputRenderer
import org.grails.scaffolding.registry.input.InListInputRenderer
import org.grails.scaffolding.registry.input.LocaleInputRenderer
import org.grails.scaffolding.registry.input.NumberInputRenderer
import org.grails.scaffolding.registry.input.StringInputRenderer
import org.grails.scaffolding.registry.input.TextareaInputRenderer
import org.grails.scaffolding.registry.input.TimeInputRenderer
import org.grails.scaffolding.registry.input.TimeZoneInputRenderer
import org.grails.scaffolding.registry.input.UrlInputRenderer
import grails.validation.Constrained
import grails.validation.ConstrainedProperty
import org.grails.datastore.mapping.model.types.OneToMany
import spock.lang.Shared
import spock.lang.Specification

import java.sql.Time

/**
 * Created by Jim on 5/26/2016.
 */
class DomainRendererRegistererSpec extends Specification {

    @Shared
    DomainInputRendererRegistry domainInputRendererRegistry

    void setup() {
        domainInputRendererRegistry = new DomainInputRendererRegistry()
        DomainOutputRendererRegistry domainOutputRendererRegistry = new DomainOutputRendererRegistry()
        new DomainRendererRegisterer(domainInputRendererRegistry: domainInputRendererRegistry, domainOutputRendererRegistry: domainOutputRendererRegistry).registerRenderers()
    }


    void "test the InList renderer is returned for String"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> String
            getConstraints() >> Stub(Constrained) {
                getInList() >> ["foo"]
            }
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof InListInputRenderer
    }

    void "test the Textarea renderer is returned"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> String
            getConstraints() >> Stub(ConstrainedProperty) {
                getWidget() >> "textarea"
            }
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof TextareaInputRenderer
    }

    void "test the String renderer is returned"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> String
            getConstraints() >> Stub(ConstrainedProperty)
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof StringInputRenderer
    }

    void "test the Boolean renderer is returned"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> Boolean
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof BooleanInputRenderer
    }

    void "test the InList renderer is returned for Number"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> Long
            getConstraints() >> Stub(Constrained) {
                getInList() >> [1L, 2L]
            }
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof InListInputRenderer
    }

    void "test the Number renderer is returned"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> Long
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof NumberInputRenderer
    }

    void "test the URL renderer is returned"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> URL
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof UrlInputRenderer
    }

    enum Fruit {APPLE,ORANGE,BANANA,PEAR};

    void "test the Enum renderer is returned"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> Fruit
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof EnumInputRenderer
    }

    void "test the Date renderer is returned"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> Calendar
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof DateInputRenderer
    }

    void "test the Time renderer is returned"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> Time
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof TimeInputRenderer
    }


    void "test the File renderer is returned"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> byte[]
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof FileInputRenderer
    }

    void "test the TimeZone renderer is returned"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> TimeZone
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof TimeZoneInputRenderer
    }

    void "test the Currency renderer is returned"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> Currency
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof CurrencyInputRenderer
    }

    void "test the Locale renderer is returned"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> Locale
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof LocaleInputRenderer
    }

    void "test the Default renderer is returned"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> Specification
            getConstraints() >> Stub(ConstrainedProperty) {
                getWidget() >> ""
            }
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof DefaultInputRenderer
    }

    void "test the BiDirectionalToMany renderer is returned"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getPersistentProperty() >> Stub(OneToMany) {
                isBidirectional() >> true
            }

        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof BidirectionalToManyInputRenderer
    }

    void "test the Association renderer is returned"() {
        given:
        DomainProperty domainProperty = Stub(DomainProperty) {
            getType() >> Set
            getPersistentProperty() >> Stub(OneToMany) {
                isBidirectional() >> false
            }
        }

        expect:
        domainInputRendererRegistry.get(domainProperty) instanceof AssociationInputRenderer
    }

}
