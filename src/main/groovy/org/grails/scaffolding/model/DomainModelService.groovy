package org.grails.scaffolding.model

import org.grails.scaffolding.model.property.DomainProperty
import org.grails.datastore.mapping.model.PersistentEntity

/**
 * An API to retrieve properties from a {@link PersistentEntity}
 *
 * @author James Kleeh
 */
interface DomainModelService {

    /**
     * @param domainClass The persistent entity
     * @return The list of {@link DomainProperty} instances that are editable
     */
    List<DomainProperty> getEditableProperties(PersistentEntity domainClass)

    /**
     * @param domainClass The persistent entity
     * @return The list of {@link DomainProperty} instances that are visible
     */
    List<DomainProperty> getVisibleProperties(PersistentEntity domainClass)

    /**
     * @param domainClass The persistent entity
     * @return The list of {@link DomainProperty} instances that are designed to be shown in a list context
     */
    List<DomainProperty> getShortListVisibleProperties(PersistentEntity domainClass)

    /**
     * @param domainClass The persistent entity
     * @param closure The closure that will be executed for each editable property
     * @return The list of editable {@link DomainProperty} instances that the closure returns true for
     */
    List<DomainProperty> findEditableProperties(PersistentEntity domainClass, Closure closure)

    /**
     * @param domainClass The persistent entity
     * @param closure The closure that will be executed for each property
     * @return Whether or not the closure returns true for any editable property
     */
    Boolean hasEditableProperty(PersistentEntity domainClass, Closure closure)

}
