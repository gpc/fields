package org.codehaus.groovy.grails.plugins.beanfields

import java.util.regex.Pattern
import org.apache.commons.lang.ClassUtils
import org.springframework.validation.FieldError
import org.springframework.beans.*

abstract class AbstractPropertyAccessor implements BeanPropertyAccessor {

	final rootBean
	final String pathFromRoot
	String propertyName
	def value
	
	AbstractPropertyAccessor(rootBean, String pathFromRoot) {
		this.rootBean = rootBean
		this.pathFromRoot = pathFromRoot
	}

	List<FieldError> getErrors() {
		rootBean.errors.getFieldErrors(pathFromRoot)
	}

	String getLabelKey() {
		"${beanType.simpleName}.${propertyName}.label"
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
		!errors.empty
	}

	List<Class> getBeanSuperclasses() {
		ClassUtils.getAllSuperclasses(beanType) - Object
	}

	protected BeanWrapper beanWrapperFor(Class type, value) {
		value ? PropertyAccessorFactory.forBeanPropertyAccess(value) : new BeanWrapperImpl(type)
	}

	protected static final Pattern INDEXED_PROPERTY_PATTERN = ~/^(\w+)\[(.+)\]$/

	protected final String stripIndex(String propertyName) {
		def matcher = propertyName =~ INDEXED_PROPERTY_PATTERN
		matcher.matches() ? matcher[0][1] : propertyName
	}
}
