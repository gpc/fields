package org.codehaus.groovy.grails.plugins.beanfields

import org.codehaus.groovy.grails.plugins.beanfields.AbstractPropertyAccessor
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.commons.*
import org.springframework.beans.*

class DomainClassPropertyAccessor extends AbstractPropertyAccessor {

	final GrailsDomainClass rootBeanClass
	GrailsDomainClass beanClass

	DomainClassPropertyAccessor(GrailsDomainClass rootBeanClass, bean, String propertyPath) {
		super(bean, propertyPath)
		this.rootBeanClass = rootBeanClass
		resolvePropertyFromPath(bean, propertyPath)
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

	String getDefaultLabel() {
		persistentProperty.naturalName
	}

	private void resolvePropertyFromPath(bean, String propertyPath) {
		def pathElements = propertyPath.tokenize(".")
		resolvePropertyFromPathComponents(rootBeanClass, PropertyAccessorFactory.forBeanPropertyAccess(bean), pathElements)
	}

	private void resolvePropertyFromPathComponents(GrailsDomainClass beanClass, BeanWrapper beanWrapper, List<String> pathElements) {
		def propertyName = pathElements.remove(0)
		def value = beanWrapper?.getPropertyValue(propertyName)
		if (pathElements.empty) {
			this.beanClass = beanClass
			this.value = value
			this.propertyName = stripIndex(propertyName)
		} else {
			def persistentProperty = beanClass.getPersistentProperty(stripIndex(propertyName))
			def propertyDomainClass = resolvePropertyDomainClass(persistentProperty)
			resolvePropertyFromPathComponents(propertyDomainClass, value ? PropertyAccessorFactory.forBeanPropertyAccess(value) : null, pathElements)
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
}
