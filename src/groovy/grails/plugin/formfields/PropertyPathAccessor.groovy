package grails.plugin.formfields

import grails.util.GrailsNameUtils
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.springframework.validation.FieldError
import static grails.plugin.formfields.BeanPropertyAccessorFactory.stripIndex
import static java.util.Collections.EMPTY_LIST
import static org.apache.commons.lang.StringUtils.substringAfterLast
import org.codehaus.groovy.grails.commons.*

class PropertyPathAccessor implements BeanPropertyAccessor {

	final String pathFromRoot
	final String propertyName = stripIndex pathFromRoot.contains('.') ? substringAfterLast(pathFromRoot, '.') : pathFromRoot
	
	PropertyPathAccessor(String pathFromRoot) {
		this.pathFromRoot = pathFromRoot
	}

	String getDefaultLabel() {
		GrailsNameUtils.getNaturalName(propertyName)
	}

	Object getRootBean() { null }
	Class getRootBeanType() { null }
	Class getBeanType() { null }
	GrailsDomainClass getBeanClass() { null }
	List<Class> getBeanSuperclasses() { EMPTY_LIST }
	Class getPropertyType() { null }
	List<Class> getPropertyTypeSuperclasses() { EMPTY_LIST }
	Object getValue() { null }
	ConstrainedProperty getConstraints() { new ConstrainedProperty(Object, propertyName, String) }
	GrailsDomainClassProperty getPersistentProperty() { null }
	List<String> getLabelKeys() { EMPTY_LIST }
	List<FieldError> getErrors() { EMPTY_LIST }
	boolean isRequired() { false }
	boolean isInvalid() { false }
}
