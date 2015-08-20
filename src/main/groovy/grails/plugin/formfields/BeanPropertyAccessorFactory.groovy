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

import groovy.transform.PackageScope
import grails.core.*
import grails.core.support.GrailsApplicationAware
import grails.core.support.proxy.ProxyHandler
import grails.validation.ConstrainedProperty
import grails.validation.ConstraintsEvaluator
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.NotReadablePropertyException
import org.springframework.beans.PropertyAccessorFactory

import java.lang.reflect.ParameterizedType
import java.util.regex.Pattern

class BeanPropertyAccessorFactory implements GrailsApplicationAware {

	GrailsApplication grailsApplication
	ConstraintsEvaluator constraintsEvaluator
	ProxyHandler proxyHandler

	BeanPropertyAccessor accessorFor(bean, String propertyPath) {
		if (bean == null) {
			new PropertyPathAccessor(propertyPath)
		} else {
			def params = [rootBean: bean, rootBeanType: bean.getClass(), pathFromRoot: propertyPath, grailsApplication: grailsApplication]
			params.rootBeanClass = resolveDomainClass(bean.getClass())

			resolvePropertyFromPath(bean, propertyPath, params)

			new BeanPropertyAccessorImpl(params)
		}
	}

	private GrailsDomainClass resolveDomainClass(Class beanClass) {
		grailsApplication.getDomainClass(beanClass.name)
	}

	private void resolvePropertyFromPath(bean, String propertyPath, Map params) {
		def beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(bean)
		def pathElements = propertyPath.tokenize(".")
		resolvePropertyFromPathComponents(beanWrapper, pathElements, params)
	}

	private void resolvePropertyFromPathComponents(BeanWrapper beanWrapper, List<String> pathElements, Map params) {
		def propertyName = pathElements.remove(0)
		def beanClass = resolveDomainClass(beanWrapper.wrappedClass)
		def propertyType = resolvePropertyType(beanWrapper, beanClass, propertyName)
		def value = beanWrapper.getPropertyValue(propertyName)
		if (pathElements.empty) {
			params.beanType = beanWrapper.wrappedClass
			params.beanClass = beanClass
			params.value = value
			params.propertyType = propertyType
			params.propertyName = stripIndex(propertyName)
			params.persistentProperty = beanClass?.getPersistentProperty(params.propertyName)
			params.constraints = resolveConstraints(beanWrapper, beanClass, params.propertyName)
		} else {
			resolvePropertyFromPathComponents(beanWrapperFor(propertyType, value), pathElements, params)
		}
	}

	private ConstrainedProperty resolveConstraints(BeanWrapper beanWrapper, GrailsDomainClass beanClass, String propertyName) {
        if (beanClass) {
			def constrainedProperty = beanClass.constrainedProperties[propertyName] as ConstrainedProperty

            if(!constrainedProperty && propertyName in beanClass.getPropertyValue(GrailsDomainClassProperty.TRANSIENT, List.class)) {
                constrainedProperty = createDefaultConstraint(beanWrapper, propertyName)
            }
            return constrainedProperty
		} else {
			// TODO: possibly a better way to get constraints direct from a command object rather than re-evaluating them
			return constraintsEvaluator.evaluate(beanWrapper.wrappedClass)[propertyName] as ConstrainedProperty ?: createDefaultConstraint(beanWrapper, propertyName)
        }
	}

    private ConstrainedProperty createDefaultConstraint(BeanWrapper beanWrapper, String propertyName) {
        def defaultConstraint = new ConstrainedProperty(beanWrapper.wrappedClass, propertyName, beanWrapper.getPropertyType(propertyName))
        defaultConstraint.nullable = true
        defaultConstraint
    }

    private Class resolvePropertyType(BeanWrapper beanWrapper, GrailsDomainClass beanClass, String propertyName) {
		Class propertyType = null
		if (beanClass) {
			propertyType = resolveDomainPropertyType(beanClass, propertyName)
		}
		if (!propertyType) {
			propertyType = resolveNonDomainPropertyType(beanWrapper, propertyName)
		}
		propertyType
	}

	private Class resolveDomainPropertyType(GrailsDomainClass beanClass, String propertyName) {
		def propertyNameWithoutIndex = stripIndex(propertyName)
		def persistentProperty = beanClass.getPersistentProperty(propertyNameWithoutIndex)
		if (!persistentProperty) throw new NotReadablePropertyException(beanClass.clazz, propertyNameWithoutIndex)
		boolean isIndexed = propertyName =~ INDEXED_PROPERTY_PATTERN
		boolean isCollection = persistentProperty.isBasicCollectionType() || persistentProperty.isAssociation()
		if (isIndexed && isCollection) {
			persistentProperty.referencedPropertyType
		} else {
			persistentProperty.type
		}
	}

	private Class resolveNonDomainPropertyType(BeanWrapper beanWrapper, String propertyName) {
		def type = beanWrapper.getPropertyType(propertyName)
		if (type == null) {
			def match = propertyName =~ INDEXED_PROPERTY_PATTERN
			if (match) {
				def genericType = beanWrapper.getPropertyDescriptor(match[0][1]).readMethod.genericReturnType
				if (genericType instanceof ParameterizedType) {
					switch (genericType.rawType) {
						case Collection:
							type = genericType.actualTypeArguments[0]
							break
						case Map:
							type = genericType.actualTypeArguments[1]
							break
					}
				} else {
					type = Object
				}
			}
		}
		type
	}

	private BeanWrapper beanWrapperFor(Class type, value) {
		value ? PropertyAccessorFactory.forBeanPropertyAccess(proxyHandler.unwrapIfProxy(value)) : new BeanWrapperImpl(type)
	}

	private static final Pattern INDEXED_PROPERTY_PATTERN = ~/^(\w+)\[(.+)\]$/

	@PackageScope
	static String stripIndex(String propertyName) {
		def matcher = propertyName =~ INDEXED_PROPERTY_PATTERN
		matcher.matches() ? matcher[0][1] : propertyName
	}
}
