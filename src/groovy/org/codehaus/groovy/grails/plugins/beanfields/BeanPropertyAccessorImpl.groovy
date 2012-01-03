package org.codehaus.groovy.grails.plugins.beanfields

import grails.util.GrailsNameUtils
import org.apache.commons.lang.ClassUtils
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.springframework.validation.FieldError
import org.apache.commons.lang.builder.*
import org.codehaus.groovy.grails.commons.*

class BeanPropertyAccessorImpl implements BeanPropertyAccessor {
	
	Object rootBean
	GrailsDomainClass rootBeanClass // TODO: not required on interface
	Class rootBeanType
	GrailsDomainClass beanClass // TODO: not required on interface
	Class beanType
	String pathFromRoot
	String propertyName
	Class propertyType
	GrailsDomainClassProperty persistentProperty
	ConstrainedProperty constraints
	Object value
	
	List<Class> getBeanSuperclasses() {
		ClassUtils.getAllSuperclasses(beanType) - Object
	}

	String getLabelKey() {
		"${beanType.simpleName}.${propertyName}.label"
	}

	String getDefaultLabel() {
		GrailsNameUtils.getNaturalName(propertyName)
	}

	List<FieldError> getErrors() {
		rootBean.errors.getFieldErrors(pathFromRoot)
	}

	boolean isRequired() {
		if (propertyType in [Boolean, boolean]) {
			false
		} else if (propertyType == String) {
			!constraints.nullable && !constraints.blank
		} else {
			!constraints.nullable
		}
	}

	boolean isInvalid() {
		!errors.isEmpty()
	}

	@Override
	int hashCode() {
		def builder = new HashCodeBuilder()
		builder.append(beanType)
		builder.append(propertyName)
		builder.append(propertyType)
		builder.toHashCode()
	}

	@Override
	boolean equals(Object obj) {
		if (!(obj instanceof BeanPropertyAccessor)) return false
		def builder = new EqualsBuilder()
		builder.append(beanType, obj.beanType)
		builder.append(propertyName, obj.propertyName)
		builder.append(propertyType, obj.propertyType)
		builder.isEquals()
	}
}
