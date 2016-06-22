package org.grails.scaffolding.model.property

import grails.core.GrailsDomainClass
import grails.util.GrailsNameUtils
import grails.validation.Constrained
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.Basic
import org.grails.validation.GrailsDomainClassValidator

import static grails.validation.ConstrainedProperty.BLANK_CONSTRAINT

/**
 * @see {@link DomainProperty}
 * @author James Kleeh
 */
@CompileStatic
class DomainPropertyImpl implements DomainProperty {

    @Delegate PersistentProperty persistentProperty
    PersistentProperty rootProperty
    protected GrailsDomainClass grailsDomainClass
    PersistentEntity domainClass
    Constrained constraints
    String pathFromRoot

    protected Boolean convertEmptyStringsToNull
    protected Boolean trimStrings

    DomainPropertyImpl(PersistentProperty persistentProperty, MappingContext mappingContext) {
        this.persistentProperty = persistentProperty
        this.domainClass = persistentProperty.owner
        this.grailsDomainClass = ((GrailsDomainClassValidator)mappingContext.getEntityValidator(domainClass))?.domainClass
        if (this.grailsDomainClass) {
            this.constraints = (Constrained)grailsDomainClass.constrainedProperties[name]
        }
        this.pathFromRoot = persistentProperty.name
    }

    DomainPropertyImpl(PersistentProperty rootProperty, PersistentProperty persistentProperty, MappingContext mappingContext) {
        this(persistentProperty, mappingContext)
        this.setRootProperty(rootProperty)
    }

    void setRootProperty(PersistentProperty rootProperty) {
        this.rootProperty = rootProperty
        this.pathFromRoot = "${rootProperty.name}.${name}"
    }

    Class getRootBeanType() {
        (rootProperty ?: persistentProperty).owner.javaClass
    }

    Class getBeanType() {
        owner.javaClass
    }

    Class getAssociatedType() {
        if (persistentProperty instanceof Association) {
            if (persistentProperty instanceof Basic) {
                ((Basic)persistentProperty).componentType
            } else {
                associatedEntity.javaClass
            }
        } else {
            null
        }
    }

    PersistentEntity getAssociatedEntity() {
        ((Association)persistentProperty).associatedEntity
    }

    boolean isRequired() {
        if (type in [Boolean, boolean]) {
            false
        } else if (type == String) {
            Constrained constraints = getConstraints()
            // if the property prohibits nulls and blanks are converted to nulls, then blanks will be prohibited even if a blank
            // constraint does not exist
            boolean hasBlankConstraint = constraints?.hasAppliedConstraint(BLANK_CONSTRAINT)
            boolean blanksImplicityProhibited = !hasBlankConstraint && !constraints?.nullable && convertEmptyStringsToNull && trimStrings
            !constraints?.nullable && (!constraints?.blank || blanksImplicityProhibited)
        } else {
            !constraints?.nullable
        }
    }

    List<String> getLabelKeys() {
        List labelKeys = []
        labelKeys.add("${GrailsNameUtils.getPropertyName(beanType.simpleName)}.${name}.label")
        if (rootProperty) {
            labelKeys.add("${GrailsNameUtils.getPropertyName(rootBeanType.simpleName)}.${pathFromRoot}.label".replaceAll(/\[(.+)\]/, ''))
        }
        labelKeys.unique()
    }

    String getDefaultLabel() {
        GrailsNameUtils.getNaturalName(name)
    }

    public int compareTo(DomainProperty o2) {

        if (domainClass.mapping.identifier.identifierName.contains(name)) {
            return -1;
        }
        if (domainClass.mapping.identifier.identifierName.contains(o2.name)) {
            return 1;
        }

        Constrained cp2 = o2.constraints

        if (constraints == null && cp2 == null) {
            return name.compareTo(o2.name);
        }

        if (constraints == null) {
            return 1;
        }

        if (cp2 == null) {
            return -1;
        }

        if (constraints.order > cp2.order) {
            return 1;
        }

        if (constraints.order < cp2.order) {
            return -1;
        }

        return 0;
    }
}
