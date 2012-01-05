package grails.plugin.formfields

import grails.util.GrailsNameUtils
import groovy.transform.Canonical
import org.apache.commons.lang.ClassUtils
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.springframework.validation.FieldError
import org.codehaus.groovy.grails.commons.*

@Canonical(includes = 'beanType, propertyName, propertyType')
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
}
