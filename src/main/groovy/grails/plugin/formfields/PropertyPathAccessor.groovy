package grails.plugin.formfields

import grails.util.GrailsNameUtils
import grails.validation.ConstrainedProperty
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.scaffolding.model.property.Constrained
import org.springframework.validation.FieldError
import static grails.plugin.formfields.BeanPropertyAccessorFactory.stripIndex
import static java.util.Collections.EMPTY_LIST
import static org.apache.commons.lang.StringUtils.substringAfterLast
import grails.core.*

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
	PersistentEntity getEntity() { null }
	List<Class> getBeanSuperclasses() { EMPTY_LIST }
	Class getPropertyType() { Object }
	List<Class> getPropertyTypeSuperclasses() { EMPTY_LIST }
	Object getValue() { null }
	Constrained getConstraints() { new Constrained(null, new ConstrainedProperty(Object, propertyName, String)) }
	GrailsDomainClassProperty getPersistentProperty() { null }
	PersistentProperty getDomainProperty() { null }
	List<String> getLabelKeys() { EMPTY_LIST }
	List<FieldError> getErrors() { EMPTY_LIST }
	boolean isRequired() { false }
	boolean isInvalid() { false }
}
