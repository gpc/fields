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

import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.springframework.validation.FieldError
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty

interface BeanPropertyAccessor {
	
	Object getRootBean()
	Class getRootBeanType()
	String getPathFromRoot()
	String getPropertyName()
	Class getBeanType()
	List<Class> getBeanSuperclasses()
	Class getPropertyType()
	List<Class> getPropertyTypeSuperclasses()
	Object getValue()
	GrailsDomainClassProperty getPersistentProperty()
	ConstrainedProperty getConstraints()
	String getLabelKey()
	String getDefaultLabel()
	List<FieldError> getErrors()
	boolean isRequired()
	boolean isInvalid()

}
