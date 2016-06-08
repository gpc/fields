package org.grails.scaffolding.model

import grails.core.GrailsDomainClass
import org.grails.scaffolding.model.property.DomainPropertyFactory
import org.grails.scaffolding.model.property.DomainPropertyFactoryImpl
import org.grails.core.DefaultGrailsDomainClass
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.validation.GrailsDomainClassValidator

trait MocksDomain {

    PersistentEntity mockDomainClass(MappingContext mappingContext, Class clazz) {
        PersistentEntity persistentEntity = mappingContext.addPersistentEntity(clazz)
        GrailsDomainClass grailsDomainClass = new DefaultGrailsDomainClass(clazz, [:])
        mappingContext.addEntityValidator(persistentEntity, new GrailsDomainClassValidator(domainClass: grailsDomainClass))
        persistentEntity
    }

    DomainPropertyFactory mockDomainPropertyFactory(MappingContext mappingContext) {
        DomainPropertyFactory domainPropertyFactory = new DomainPropertyFactoryImpl()
        domainPropertyFactory.trimStrings = true
        domainPropertyFactory.convertEmptyStringsToNull = true
        domainPropertyFactory.grailsDomainClassMappingContext = mappingContext
        domainPropertyFactory
    }
}