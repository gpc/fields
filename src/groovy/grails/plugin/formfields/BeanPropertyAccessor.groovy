package grails.plugin.formfields

import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.springframework.validation.FieldError

interface BeanPropertyAccessor {
	
	Object getRootBean()
	Class getRootBeanType()
	String getPathFromRoot()
	String getPropertyName()
	Class getBeanType()
	List<Class> getBeanSuperclasses()
	Class getPropertyType()
	Object getValue()
	ConstrainedProperty getConstraints()
	String getLabelKey()
	String getDefaultLabel()
	List<FieldError> getErrors()
	boolean isRequired()
	boolean isInvalid()

}
