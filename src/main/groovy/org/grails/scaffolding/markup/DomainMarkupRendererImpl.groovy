package org.grails.scaffolding.markup

import org.grails.scaffolding.model.DomainModelService
import org.grails.scaffolding.model.property.DomainProperty
import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder
import org.grails.buffer.FastStringWriter
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Embedded
import org.springframework.beans.factory.annotation.Autowired

/**
 * @see {@link DomainMarkupRenderer}
 * @author James Kleeh
 */
@CompileStatic
class DomainMarkupRendererImpl implements DomainMarkupRenderer {

    @Autowired
    DomainModelService domainModelService

    @Autowired
    PropertyMarkupRenderer propertyMarkupRenderer

    @Autowired
    ContextMarkupRenderer contextMarkupRenderer

    static void callWithDelegate(delegate, Closure closure) {
        closure.delegate = delegate
        closure.call()
    }

    static String outputMarkupContent(Closure closure) {
        FastStringWriter writer = new FastStringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(writer)
        markupBuilder.doubleQuotes = true
        markupBuilder.escapeAttributes = false
        closure.delegate = markupBuilder
        if (closure.maximumNumberOfParameters == 1) {
            closure.call(markupBuilder)
        } else {
            closure.call()
        }
        writer.toString()
    }

    protected Closure renderInput(DomainProperty property) {
        contextMarkupRenderer.inputContext(property, propertyMarkupRenderer.renderInput(property))
    }

    protected Closure renderOutput(DomainProperty property) {
        contextMarkupRenderer.outputContext(property, propertyMarkupRenderer.renderOutput(property))
    }

    String renderListOutput(PersistentEntity domainClass) {
        List<DomainProperty> tableProperties = domainModelService.getListOutputProperties(domainClass)
        outputMarkupContent (
            contextMarkupRenderer.listOutputContext(domainClass, tableProperties) { DomainProperty domainProperty ->
                propertyMarkupRenderer.renderListOutput(domainProperty)
            }
        )
    }

    String renderInput(PersistentEntity domainClass) {
        outputMarkupContent(
            contextMarkupRenderer.inputContext(domainClass) { ->
                def contextDelegate = delegate
                domainModelService.getInputProperties(domainClass).each { DomainProperty property ->
                    if (property.persistentProperty instanceof Embedded) {
                        callWithDelegate(contextDelegate, contextMarkupRenderer.embeddedInputContext(property) {
                            domainModelService.getInputProperties(((Embedded)property.persistentProperty).associatedEntity).each { DomainProperty embedded ->
                                embedded.rootProperty = property
                                callWithDelegate(contextDelegate, renderInput(embedded))
                            }
                        })
                    } else {
                        callWithDelegate(contextDelegate, renderInput(property))
                    }
                }
            }
        )
    }

    String renderOutput(PersistentEntity domainClass) {
        outputMarkupContent(
            contextMarkupRenderer.outputContext(domainClass) { ->
                def contextDelegate = delegate
                domainModelService.getOutputProperties(domainClass).each { DomainProperty property ->
                    if (property.persistentProperty instanceof Embedded) {
                        callWithDelegate(contextDelegate, contextMarkupRenderer.embeddedOutputContext(property) { ->
                            domainModelService.getOutputProperties(((Embedded)property.persistentProperty).associatedEntity).each { DomainProperty embedded ->
                                embedded.rootProperty = property
                                callWithDelegate(contextDelegate, renderOutput(embedded))
                            }
                        })
                    } else {
                        callWithDelegate(contextDelegate, renderOutput(property))
                    }
                }
            }
        )
    }
}
