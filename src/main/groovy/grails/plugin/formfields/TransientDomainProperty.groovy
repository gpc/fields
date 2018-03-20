package grails.plugin.formfields

import grails.util.GrailsNameUtils
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.scaffolding.model.property.Constrained
import org.grails.scaffolding.model.property.DomainProperty

class TransientDomainProperty implements DomainProperty {

    @Delegate PersistentProperty persistentProperty
    PersistentProperty rootProperty
    PersistentEntity domainClass
    Constrained constrained
    String pathFromRoot

    TransientDomainProperty(PersistentEntity entity, String property) {
        this.persistentProperty = null
        this.domainClass = entity
        this.constrained = null
        this.pathFromRoot = property
    }

    @Override
    Class getRootBeanType() {
        domainClass.javaClass
    }

    @Override
    Class getBeanType() {
        domainClass.javaClass
    }

    @Override
    Class getAssociatedType() {
        null
    }

    @Override
    PersistentEntity getAssociatedEntity() {
        null
    }

    @Override
    boolean isRequired() {
        false
    }

    @Override
    List<String> getLabelKeys() {
        List labelKeys = []
        labelKeys.add("${GrailsNameUtils.getPropertyName(beanType.simpleName)}.${name}.label")
        labelKeys
    }

    @Override
    String getDefaultLabel() {
        GrailsNameUtils.getNaturalName(name)
    }

    @Override
    int compareTo(DomainProperty o2) {

        if (domainClass.mapping.identifier?.identifierName?.contains(name)) {
            return -1
        }
        if (domainClass.mapping.identifier?.identifierName?.contains(o2.name)) {
            return 1
        }

        Constrained cp2 = o2.constrained

        if (constrained == null  && cp2 == null) {
            return name <=> o2.name
        }

        if (constrained == null) {
            return 1
        }

        if (cp2 == null) {
            return -1
        }

        if (constrained.order > cp2.order) {
            return 1
        }

        if (constrained.order < cp2.order) {
            return -1
        }

        return 0
    }
}
