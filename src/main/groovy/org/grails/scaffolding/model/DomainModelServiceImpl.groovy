package org.grails.scaffolding.model

import org.grails.scaffolding.model.property.DomainProperty
import org.grails.scaffolding.model.property.DomainPropertyFactory
import grails.util.GrailsClassUtils
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Embedded
import org.springframework.beans.factory.annotation.Autowired

/**
 * @see {@link DomainModelService}
 * @author James Kleeh
 */
@CompileStatic
class DomainModelServiceImpl implements DomainModelService {

    @Autowired
    DomainPropertyFactory domainPropertyFactory

    List<DomainProperty> getEditableProperties(PersistentEntity domainClass) {
        List<DomainProperty> properties = domainClass.persistentProperties.collect {
            domainPropertyFactory.build(it)
        }
        List blacklist = ['version', 'dateCreated', 'lastUpdated']
        Object scaffoldProp = GrailsClassUtils.getStaticPropertyValue(domainClass.javaClass, 'scaffold')
        if (scaffoldProp instanceof Map) {
            Map scaffold = (Map)scaffoldProp
            if (scaffold.containsKey('exclude')) {
                if (scaffold.exclude instanceof Collection) {
                    blacklist.addAll((Collection)scaffold.exclude)
                } else if (scaffold.exclude instanceof String) {
                    blacklist.add((String)scaffold.exclude)
                }
            }
        }
        properties.removeAll { it.name in blacklist }
        properties.removeAll { !it.constraints.display }
        //TODO Wait for Graeme to implement access to determine if a property is derived
        //properties.removeAll { it.mapping instanceof PropertyConfig classMapping.mappedForm..properties.derived }
        properties.sort()
        properties
    }

    List<DomainProperty> getVisibleProperties(PersistentEntity domainClass) {
        List<DomainProperty> properties = domainClass.persistentProperties.collect {
            domainPropertyFactory.build(it)
        }
        properties.removeAll { it.name == 'version' }
        properties.sort()
        properties
    }

    List<DomainProperty> getShortListVisibleProperties(PersistentEntity domainClass) {
        List<DomainProperty> properties = getVisibleProperties(domainClass)
        if (properties.size() > 5) {
            properties = properties[0..5]
        }
        properties.add(0, domainPropertyFactory.build(domainClass.identity))
        properties
    }

    List<DomainProperty> findEditableProperties(PersistentEntity domainClass, Closure closure) {
        List<DomainProperty> properties = []
        getEditableProperties(domainClass).each { DomainProperty domainProperty ->
            PersistentProperty property = domainProperty.persistentProperty
            if (property instanceof Embedded) {
                getEditableProperties(((Embedded)property).associatedEntity).each { DomainProperty embedded ->
                    embedded.rootProperty = domainProperty
                    if (closure.call(embedded)) {
                        properties.add(embedded)
                    }
                }
            } else {
                if (closure.call(domainProperty)) {
                    properties.add(domainProperty)
                }
            }
        }
        properties
    }

    Boolean hasEditableProperty(PersistentEntity domainClass, Closure closure) {
        findEditableProperties(domainClass, closure).size() > 0
    }

}
