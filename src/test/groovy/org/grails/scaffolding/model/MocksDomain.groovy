package org.grails.scaffolding.model

import grails.core.GrailsDomainClass
import grails.gorm.validation.PersistentEntityValidator
import org.grails.datastore.gorm.validation.constraints.registry.DefaultConstraintRegistry
import org.grails.datastore.gorm.validation.constraints.registry.DefaultValidatorRegistry
import org.grails.scaffolding.model.property.DomainPropertyFactory
import org.grails.scaffolding.model.property.DomainPropertyFactoryImpl
import org.grails.core.DefaultGrailsDomainClass
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import org.grails.validation.GrailsDomainClassValidator
import org.spockframework.mock.ISpockMockObject
import org.springframework.context.MessageSource
import org.springframework.validation.Validator
import spock.lang.MockingApi

trait MocksDomain {

    PersistentEntity mockDomainClass(MappingContext mappingContext, Class clazz) {
        PersistentEntity persistentEntity = mappingContext.addPersistentEntity(clazz)
        GrailsDomainClass grailsDomainClass = new DefaultGrailsDomainClass(clazz, [:])
        mappingContext.addEntityValidator(persistentEntity, new GrailsDomainClassValidator(domainClass: grailsDomainClass))
        persistentEntity
    }

    PersistentEntity mockDomainClassEntityValidator(MappingContext mappingContext, Class clazz) {
        PersistentEntity persistentEntity = mappingContext.addPersistentEntity(clazz)
        Validator validator = new DefaultValidatorRegistry(mappingContext).getValidator(persistentEntity)
        mappingContext.addEntityValidator(persistentEntity, validator)
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