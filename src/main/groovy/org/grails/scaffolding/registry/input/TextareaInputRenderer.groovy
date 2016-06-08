package org.grails.scaffolding.registry.input

import org.grails.scaffolding.model.property.DomainProperty
import org.grails.scaffolding.registry.DomainInputRenderer

/**
 * The default renderer for rendering properties with the constraint {@code [widget: "textarea"]}
 *
 * @author James Kleeh
 */
class TextareaInputRenderer implements DomainInputRenderer {

    @Override
    boolean supports(DomainProperty domainProperty) {
        domainProperty.constraints?.widget == "textarea"
    }

    @Override
    Closure renderInput(Map defaultAttributes, DomainProperty domainProperty) {
        Integer maxSize = domainProperty.constraints?.maxSize
        if (maxSize) {
            defaultAttributes.maxlength = maxSize
        }
        return { ->
            textarea(defaultAttributes)
        }
    }
}
