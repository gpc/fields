package org.codehaus.groovy.grails.plugins.beanfields

import org.apache.commons.lang.ClassUtils
import org.springframework.validation.FieldError
import java.util.regex.Pattern

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
		ClassUtils.getAllSuperclasses(beanType) - Object
	}

	private static final Pattern INDEXED_PROPERTY_PATTERN = ~/^(\w+)\[(.+)\]$/

	protected final String stripIndex(String propertyName) {
		def matcher = propertyName =~ INDEXED_PROPERTY_PATTERN
		matcher.matches() ? matcher[0][1] : propertyName
	}
}
