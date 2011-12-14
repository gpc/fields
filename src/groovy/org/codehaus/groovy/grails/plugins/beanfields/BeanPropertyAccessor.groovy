package org.codehaus.groovy.grails.plugins.beanfields

import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.springframework.validation.FieldError

interface BeanPropertyAccessor {
	
	Object getRootBean()
	Class getRootBeanType()
	String getPathFromRoot()
	String getPropertyName()
	void setPropertyName(String propertyName)
	Class getBeanType()
	List<Class> getBeanSuperclasses()
	Class getType()
	Object getValue()
	void setValue(Object value)
	ConstrainedProperty getConstraints()
	String getLabelKey()
	String getDefaultLabel()
	List<FieldError> getErrors()
	boolean isRequired()
	boolean isInvalid()

}
