package org.codehaus.groovy.grails.plugins.beanfields

import java.util.regex.Pattern
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.springframework.validation.FieldError
import org.codehaus.groovy.grails.commons.*
import org.springframework.beans.*
import org.apache.commons.lang.ClassUtils

interface BeanPropertyAccessor {
	
	Object getRootBean()
	Class getRootBeanType()
	String getPathFromRoot()
	String getPropertyName()
	Class getBeanType()
	List<Class> getBeanSuperclasses()
	Class getType()
	Object getValue()
	ConstrainedProperty getConstraints()
	String getLabelKey()
	String getDefaultLabel()
	List<FieldError> getErrors()
	boolean isRequired()
	boolean isInvalid()

}

class DomainClassPropertyAccessor implements BeanPropertyAccessor {

	final rootBean
	final GrailsDomainClass rootBeanClass
	final String pathFromRoot
	GrailsDomainClass beanClass
	String propertyName
	def value

	DomainClassPropertyAccessor(rootBean, GrailsDomainClass rootBeanClass, String pathFromRoot) {
		this.rootBean = rootBean
		this.rootBeanClass = rootBeanClass
		this.pathFromRoot = pathFromRoot
		this.beanClass = beanClass
		this.propertyName = propertyName
		this.value = value
	}

	Class getRootBeanType() {
		rootBeanClass.clazz
	}

	Class getBeanType() {
		beanClass.clazz
	}

	Class getType() {
		boolean isIndexed = pathFromRoot =~ /\w+\[.*?\]$/
		boolean isCollection = persistentProperty.isBasicCollectionType() || persistentProperty.isAssociation()
		if (isIndexed && isCollection) {
			persistentProperty.referencedPropertyType
		} else {
			persistentProperty.type
		}
	}

	GrailsDomainClassProperty getPersistentProperty() {
		beanClass.getPersistentProperty(propertyName)
	}

	ConstrainedProperty getConstraints() {
		beanClass.constrainedProperties[propertyName]
	}

	String getLabelKey() {
		"${beanClass.clazz.simpleName}.${propertyName}.label"
	}

	String getDefaultLabel() {
		persistentProperty.naturalName
	}

	List<FieldError> getErrors() {
		rootBean.errors.getFieldErrors(pathFromRoot)
	}

	boolean isRequired() {
		if (type in [Boolean, boolean]) {
			false
		} else if (type == String) {
			!constraints.nullable && !constraints.blank
		} else {
			!constraints.nullable
		}
	}

	boolean isInvalid() {
		!errors.empty
	}
	
	List<Class> getBeanSuperclasses() {
		ClassUtils.getAllSuperclasses(beanClass.clazz) - Object
	}

}

class BeanPropertyAccessorFactory implements GrailsApplicationAware {

	private static final Pattern INDEXED_PROPERTY_PATTERN = ~/^(\w+)\[(.+)\]$/

	GrailsApplication grailsApplication

	BeanPropertyAccessor accessorFor(bean, String propertyPath) {
		def rootBeanClass = resolveDomainClass(bean.getClass())
		def propertyAccessor = new DomainClassPropertyAccessor(bean, rootBeanClass, propertyPath)
		def pathElements = propertyPath.tokenize(".")
		resolvePropertyFromPathComponents(propertyAccessor, PropertyAccessorFactory.forBeanPropertyAccess(bean), rootBeanClass, pathElements)
	}

	private BeanPropertyAccessor resolvePropertyFromPathComponents(BeanPropertyAccessor propertyAccessor, BeanWrapper beanWrapper, GrailsDomainClass beanClass, List<String> pathElements) {
		def propertyName = pathElements.remove(0)
		def value = beanWrapper?.getPropertyValue(propertyName)
		if (pathElements.empty) {
			propertyAccessor.beanClass = beanClass
			propertyAccessor.value = value
			propertyAccessor.propertyName = stripIndex(propertyName)
			return propertyAccessor
		} else {
			def persistentProperty = beanClass.getPersistentProperty(stripIndex(propertyName))
			def propertyDomainClass = resolvePropertyDomainClass(persistentProperty)
			return resolvePropertyFromPathComponents(propertyAccessor, value ? PropertyAccessorFactory.forBeanPropertyAccess(value) : null, propertyDomainClass, pathElements)
		}
	}

	private GrailsDomainClass resolvePropertyDomainClass(GrailsDomainClassProperty persistentProperty) {
		if (persistentProperty.embedded) {
			persistentProperty.component
		} else if (persistentProperty.association) {
			persistentProperty.referencedDomainClass
		} else {
			null
		}
	}

	private GrailsDomainClass resolveDomainClass(Class beanClass) {
		grailsApplication.getDomainClass(beanClass.name)
	}

	private String stripIndex(String propertyName) {
		def matcher = propertyName =~ INDEXED_PROPERTY_PATTERN
		matcher.matches() ? matcher[0][1] : propertyName
	}
}
