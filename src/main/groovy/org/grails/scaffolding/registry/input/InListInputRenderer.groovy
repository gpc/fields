package org.grails.scaffolding.registry.input

import org.grails.scaffolding.model.property.DomainProperty
import org.grails.scaffolding.registry.DomainInputRenderer

/**
 * The default renderer for rendering properties with an inList constraint
 *
 * @author James Kleeh
 */
class InListInputRenderer implements DomainInputRenderer {

    @Override
    boolean supports(DomainProperty domainProperty) {
        domainProperty.constraints?.inList
    }

    @Override
    Closure renderInput(Map standardAttributes, DomainProperty domainProperty) {
        List inList = domainProperty.constraints?.inList

        return { ->
            select(standardAttributes) {
                inList.each {
                    option(it, [value: it])
                }
            }
        }
    }
}
