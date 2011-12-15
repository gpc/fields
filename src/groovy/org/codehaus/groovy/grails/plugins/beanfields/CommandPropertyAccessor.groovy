package org.codehaus.groovy.grails.plugins.beanfields

import grails.util.GrailsNameUtils
import org.codehaus.groovy.grails.plugins.beanfields.AbstractPropertyAccessor
import org.codehaus.groovy.grails.validation.*
import org.springframework.beans.*

class CommandPropertyAccessor extends AbstractPropertyAccessor {
	
	final Class rootBeanType
	Class beanType
	Class type
	
	private final ConstraintsEvaluator constraintsEvaluator

	CommandPropertyAccessor(bean, String propertyPath, ConstraintsEvaluator constraintsEvaluator) {
		super(bean, propertyPath)
		this.rootBeanType = bean.getClass()
		this.constraintsEvaluator = constraintsEvaluator
		resolvePropertyFromPath(bean, propertyPath)
	}

	ConstrainedProperty getConstraints() {
		def constraints = constraintsEvaluator.evaluate(beanType)
		constraints[propertyName]
	}

	String getDefaultLabel() {
		GrailsNameUtils.getNaturalName(propertyName)
	}

	private void resolvePropertyFromPath(bean, String propertyPath) {
		def pathElements = propertyPath.tokenize(".")
		resolvePropertyFromPathComponents(PropertyAccessorFactory.forBeanPropertyAccess(bean), pathElements)
	}

	private void resolvePropertyFromPathComponents(BeanWrapper beanWrapper, List<String> pathElements) {
		def propertyName = pathElements.remove(0)
		def type = beanWrapper.getPropertyType(propertyName)
		def value = beanWrapper.getPropertyValue(propertyName)
		if (pathElements.empty) {
			this.beanType = beanWrapper.wrappedClass
			this.value = value
			this.type = type
			this.propertyName = stripIndex(propertyName)
		} else {
			resolvePropertyFromPathComponents(value ? PropertyAccessorFactory.forBeanPropertyAccess(value) : new BeanWrapperImpl(type), pathElements)
		}
	}
}
