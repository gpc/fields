package grails.plugin.formfields

import grails.core.GrailsApplication
import grails.core.GrailsDomainClass
import grails.gorm.validation.PersistentEntityValidator
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.validation.DefaultDomainClassValidator
import org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator
import org.grails.datastore.gorm.validation.constraints.registry.DefaultConstraintRegistry
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.validation.GrailsDomainClassValidator
import org.springframework.context.MessageSource
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.StaticMessageSource
import org.springframework.validation.Validator

class MappingContextBuilder {

    private GrailsApplication grailsApplication
    private MappingContext mappingContext
    private DefaultConstraintEvaluator constraintEvaluator
    private MessageSource messageSource

    MappingContextBuilder(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication

        this.mappingContext = new KeyValueMappingContext(UUID.randomUUID().toString())
        this.messageSource = new StaticMessageSource()
        DefaultConstraintRegistry constraintRegistry = new DefaultConstraintRegistry(messageSource)
        constraintEvaluator = new DefaultConstraintEvaluator(constraintRegistry, mappingContext, [:])
    }

    MappingContext build(Class...classes) {
        Collection<PersistentEntity> entities = mappingContext.addPersistentEntities(classes)
        for (PersistentEntity entity in entities) {
            entity.initialize()
            GrailsDomainClass gdc = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, entity.javaClass.name)
            Validator validator
            if (gdc) {
                validator = new GrailsDomainClassValidator()
                validator.grailsApplication = grailsApplication
                validator.messageSource = messageSource
                validator.domainClass = gdc
            }
            else {
                validator = new PersistentEntityValidator(entity, messageSource, constraintEvaluator)
            }

            mappingContext.addEntityValidator(entity, validator)
        }
        mappingContext
    }

    MappingContext build() {
        build(grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE)*.clazz as Class[])
    }
}
