package grails.plugin.formfields

import grails.core.GrailsDomainClass
import grails.core.GrailsDomainClassProperty
import grails.gorm.Entity
import grails.util.GrailsNameUtils
import grails.validation.ConstrainedProperty
import grails.validation.Validateable
import grails.web.databinding.WebDataBinding
import groovy.transform.CompileStatic
import org.apache.commons.lang.ClassUtils
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.gorm.GormValidateable
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.scaffolding.model.property.DomainProperty
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

@CompileStatic
class DelegatingBeanPropertyAccessorImpl implements BeanPropertyAccessor {

    private DomainProperty domainProperty
    private Object rootBean
    private Object value
    private String pathFromRoot
    private Class propertyType

    DelegatingBeanPropertyAccessorImpl(Object rootBean, Object value, Class propertyType, String pathFromRoot, DomainProperty domainProperty) {
        this.rootBean = rootBean
        this.value = value
        this.pathFromRoot = pathFromRoot
        this.domainProperty = domainProperty
        this.propertyType = propertyType
    }

    @Override
    Object getRootBean() {
        rootBean
    }

    @Override
    Class getRootBeanType() {
        rootBean.getClass()
    }

    @Override
    String getPathFromRoot() {
        pathFromRoot
    }

    @Override
    String getPropertyName() {
        domainProperty.name
    }

    @Override
    Class getBeanType() {
        domainProperty.beanType
    }

    @Override
    @Deprecated
    GrailsDomainClass getBeanClass() {
        throw new UnsupportedOperationException()
    }

    @Override
    PersistentEntity getEntity() {
        domainProperty.domainClass
    }

    @Override
    List<Class> getBeanSuperclasses() {
        getSuperclassesAndInterfaces(beanType)
    }

    @Override
    Class getPropertyType() {
        propertyType
    }

    @Override
    List<Class> getPropertyTypeSuperclasses() {
        getSuperclassesAndInterfaces(propertyType)
    }

    @Override
    Object getValue() {
        value
    }

    @Override
    @Deprecated
    GrailsDomainClassProperty getPersistentProperty() {
        throw new UnsupportedOperationException()
    }

    @Override
    PersistentProperty getDomainProperty() {
        domainProperty.persistentProperty
    }

    @Override
    ConstrainedProperty getConstraints() {
        (ConstrainedProperty)domainProperty.constraints
    }

    @Override
    List<String> getLabelKeys() {
        List labelKeys = []
        if (rootBean) {
            labelKeys.add("${GrailsNameUtils.getPropertyName(rootBeanType.simpleName)}.${pathFromRoot}.label".replaceAll(/\[(.+)\]/, ''))
        }
        labelKeys.addAll(domainProperty.labelKeys)
        labelKeys.unique()
    }

    @Override
    String getDefaultLabel() {
        domainProperty.defaultLabel
    }

    @Override
    List<FieldError> getErrors() {
        Errors errors
        if (rootBean instanceof Validateable) {
            errors = ((Validateable)rootBean).errors
        } else if (rootBean instanceof GormValidateable) {
            errors = ((GormValidateable)rootBean).errors
        }
        if (errors) {
            errors.getFieldErrors(pathFromRoot)
        } else {
            []
        }
    }

    @Override
    boolean isRequired() {
        domainProperty.required
    }

    @Override
    boolean isInvalid() {
        !errors.isEmpty()
    }

    private List<Class> getSuperclassesAndInterfaces(Class type) {
        List<Class> superclasses = []
        superclasses.addAll(ClassUtils.getAllSuperclasses(ClassUtils.primitiveToWrapper(type)))
        for(Object it in ClassUtils.getAllInterfaces(type)) {
            Class interfaceCls = (Class)it
            String name = interfaceCls.name
            if(name.indexOf('$') == -1) {
                if(interfaceCls.package != GormEntity.package) {
                    superclasses.add(interfaceCls)
                }
            }
        }
        superclasses.removeAll([Object, GroovyObject, Serializable, Cloneable, Comparable, WebDataBinding, DirtyCheckable, Entity])
        return superclasses.unique()
    }
}
