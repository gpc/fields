/*
 * Copyright 2012 Rob Fletcher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
	GrailsDomainClass rootBeanClass
	Class rootBeanType
	GrailsDomainClass beanClass
	Class beanType
	String pathFromRoot
	String propertyName
	Class propertyType
	GrailsDomainClassProperty persistentProperty
	ConstrainedProperty constraints
	Object value
	
	List<Class> getBeanSuperclasses() {
		getSuperclassesAndInterfaces(beanType)
	}
	
	List<Class> getPropertyTypeSuperclasses() {
		getSuperclassesAndInterfaces(propertyType)
	}

	List<String> getLabelKeys() {
		[
			"${GrailsNameUtils.getPropertyName(rootBeanType.simpleName)}.${pathFromRoot}.label".replaceAll(/\[(.+)\]/, ''),
			"${GrailsNameUtils.getPropertyName(beanType.simpleName)}.${propertyName}.label"
		].unique()
	}

	String getDefaultLabel() {
		GrailsNameUtils.getNaturalName(propertyName)
	}

	List<FieldError> getErrors() {
		if (rootBean.metaClass.hasProperty(rootBean, 'errors')) {
			rootBean.errors.getFieldErrors(pathFromRoot)
		} else {
			[]
		}
	}

	boolean isRequired() {
		if (propertyType in [Boolean, boolean]) {
			false
		} else if (propertyType == String) {
			!constraints?.nullable && !constraints?.blank
		} else {
			!constraints?.nullable
		}
	}

	boolean isInvalid() {
		!errors.isEmpty()
	}

	private List<Class> getSuperclassesAndInterfaces(Class type) {
		def superclasses = []
		superclasses.addAll(ClassUtils.getAllSuperclasses(type))
		superclasses.addAll(ClassUtils.getAllInterfaces(type))
		superclasses.removeAll([Object, GroovyObject, Serializable, Cloneable, Comparable])
		superclasses
	}
}
