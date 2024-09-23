package org.grails.scaffolding.model

import org.grails.datastore.mapping.config.Property
import org.grails.scaffolding.model.property.Constrained
import org.grails.scaffolding.model.property.DomainProperty
import org.grails.scaffolding.model.property.DomainPropertyFactory
import grails.util.GrailsClassUtils
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Embedded
import org.springframework.beans.factory.annotation.Autowired

import java.lang.reflect.Method

/**
 * @see {@link DomainModelService}
 * @author James Kleeh
 */
@CompileStatic
class DomainModelServiceImpl implements DomainModelService {

    @Autowired
    DomainPropertyFactory domainPropertyFactory

    private static Method derivedMethod

    static {
        try {
            derivedMethod = Property.class.getMethod("isDerived", (Class<?>[]) null)
        } catch (NoSuchMethodException | SecurityException e) {
            // no-op
        }
    }

    /**
     * <p>Retrieves persistent properties and excludes:<ul>
     * <li>Any properties listed in the {@code static scaffold = [exclude: []]} property on the domain class
     * <li>Any properties that have the constraint {@code [display: false]}
     * <li>Any properties whose name exist in the blackList
     * </ul><p>
     *
     * @see {@link DomainModelService#getInputProperties}
     * @param domainClass The persistent entity
     * @param blackList The list of domain class property names to exclude
     */
    protected List<DomainProperty> getProperties(PersistentEntity domainClass, List<String> blacklist) {
        List<DomainProperty> properties = domainClass.persistentProperties.collect {
            domainPropertyFactory.build(it)
        }
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

        properties.removeAll {
            if (it.name in blacklist) {
                return true
            }
            Constrained constrained = it.constrained
            if (constrained && !constrained.display) {
                return true
            }
            if (derivedMethod != null) {
                Property property = it.mapping.mappedForm
                if (derivedMethod.invoke(property, (Object[]) null)) {
                    return true
                }
            }

            false
        }
        properties.sort()
        properties
    }

    /**
     * <p>Blacklist:<ul>
     * <li>version
     * <li>dateCreated
     * <li>lastUpdated
     * </ul><p>
     *
     * @see {@link DomainModelServiceImpl#getProperties}
     * @param domainClass The persistent entity
     */
    List<DomainProperty> getInputProperties(PersistentEntity domainClass) {
        getProperties(domainClass, ['version', 'dateCreated', 'lastUpdated'])
    }

    /**
     * <p>Blacklist:<ul>
     * <li>version
     * </ul><p>
     *
     * @see {@link DomainModelServiceImpl#getProperties}
     * @param domainClass The persistent entity
     */
    List<DomainProperty> getOutputProperties(PersistentEntity domainClass) {
        getProperties(domainClass, ['version'])
    }

    /**
     * <p>The same as {@link #getOutputProperties(org.grails.datastore.mapping.model.PersistentEntity)} except the identifier is prepended<p>
     *
     * @see {@link DomainModelServiceImpl#getOutputProperties}
     * @param domainClass The persistent entity
     */
    List<DomainProperty> getListOutputProperties(PersistentEntity domainClass) {
        List<DomainProperty> properties = getOutputProperties(domainClass)
        properties.add(0, domainPropertyFactory.build(domainClass.identity))
        properties
    }

    /**
     * Will return all properties in a domain class that the provided closure returns
     * true for. Searches embedded properties
     *
     * @see {@link DomainModelService#findInputProperties}
     * @param domainClass The persistent entity
     * @param closure The closure that will be executed for each editable property
     */
    List<DomainProperty> findInputProperties(PersistentEntity domainClass, Closure closure) {
        List<DomainProperty> properties = []
        getInputProperties(domainClass).each { DomainProperty domainProperty ->
            PersistentProperty property = domainProperty.persistentProperty
            if (property instanceof Embedded) {
                getInputProperties(((Embedded)property).associatedEntity).each { DomainProperty embedded ->
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

    /**
     * Returns true if the provided closure returns true for any domain class
     * property. Searches embedded properties
     *
     * @see {@link DomainModelService#hasInputProperty}
     * @param domainClass The persistent entity
     * @param closure The closure that will be executed for each editable property
     */
    Boolean hasInputProperty(PersistentEntity domainClass, Closure closure) {
        findInputProperties(domainClass, closure).size() > 0
    }

}
