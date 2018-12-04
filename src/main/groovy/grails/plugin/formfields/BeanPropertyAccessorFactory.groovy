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

import grails.gorm.validation.DefaultConstrainedProperty
import groovy.transform.PackageScope
import grails.core.*
import grails.core.support.GrailsApplicationAware
import grails.core.support.proxy.ProxyHandler
import org.grails.datastore.gorm.validation.constraints.eval.ConstraintsEvaluator
import org.grails.datastore.gorm.validation.constraints.registry.DefaultConstraintRegistry
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.Basic
import org.grails.scaffolding.model.property.Constrained
import org.grails.scaffolding.model.property.DomainProperty
import org.grails.scaffolding.model.property.DomainPropertyFactory
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.context.support.StaticMessageSource

import java.lang.reflect.ParameterizedType
import java.util.regex.Pattern

class BeanPropertyAccessorFactory implements GrailsApplicationAware {

	GrailsApplication grailsApplication
	ConstraintsEvaluator constraintsEvaluator
	ProxyHandler proxyHandler
	DomainPropertyFactory fieldsDomainPropertyFactory
	MappingContext grailsDomainClassMappingContext

	BeanPropertyAccessor accessorFor(bean, String propertyPath) {
		if (bean == null) {
			new PropertyPathAccessor(propertyPath)
		} else {
			resolvePropertyFromPath(bean, propertyPath)
		}
	}

	private PersistentEntity resolveDomainClass(Class beanClass) {
		grailsDomainClassMappingContext.getPersistentEntity(beanClass.name)
	}

	private BeanPropertyAccessor resolvePropertyFromPath(bean, String pathFromRoot) {
		def beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(bean)
		def pathElements = pathFromRoot.tokenize(".")

		def params = [rootBean: bean, rootBeanType: bean.getClass(), pathFromRoot: pathFromRoot, grailsApplication: grailsApplication]

		DomainProperty domainProperty = resolvePropertyFromPathComponents(beanWrapper, pathElements, params)

		if (domainProperty != null) {
			new DelegatingBeanPropertyAccessorImpl(bean, params.value, params.propertyType, pathFromRoot, domainProperty)
		} else {
			new BeanPropertyAccessorImpl(params)
		}

	}

	private DomainProperty resolvePropertyFromPathComponents(BeanWrapper beanWrapper, List<String> pathElements, params) {
		def propertyName = pathElements.remove(0)
		PersistentEntity beanClass = resolveDomainClass(beanWrapper.wrappedClass)
		def propertyType = resolvePropertyType(beanWrapper, beanClass, propertyName)
		def value = beanWrapper.getPropertyValue(propertyName)
		if (pathElements.empty) {
			params.value = value
			params.propertyType = propertyType

			PersistentProperty persistentProperty
			String nameWithoutIndex = stripIndex(propertyName)
			if (beanClass != null) {
				persistentProperty = beanClass.getPropertyByName(nameWithoutIndex)
				if (!persistentProperty && beanClass.isIdentityName(nameWithoutIndex)) {
					persistentProperty = beanClass.identity
				}
			}

			if (persistentProperty != null) {
				fieldsDomainPropertyFactory.build(persistentProperty)
			} else {
				params.entity = beanClass
				params.beanType = beanWrapper.wrappedClass
				params.propertyType = propertyType
				params.propertyName = nameWithoutIndex
				params.domainProperty = null
				params.constraints = resolveConstraints(beanWrapper, params.propertyName)
				null
			}
		} else {
			resolvePropertyFromPathComponents(beanWrapperFor(propertyType, value), pathElements, params)
		}
	}

	private Constrained resolveConstraints(BeanWrapper beanWrapper, String propertyName) {
		grails.gorm.validation.Constrained constraint = constraintsEvaluator.evaluate(beanWrapper.wrappedClass)[propertyName]
		if (!constraint) {
			constraint = createDefaultConstraint(beanWrapper, propertyName)
		}
		new Constrained(constraint)
	}

    private grails.gorm.validation.Constrained createDefaultConstraint(BeanWrapper beanWrapper, String propertyName) {
        def defaultConstraint = new DefaultConstrainedProperty(beanWrapper.wrappedClass, propertyName, beanWrapper.getPropertyType(propertyName), new DefaultConstraintRegistry(new StaticMessageSource()))
        defaultConstraint.nullable = true
		defaultConstraint
    }

    private Class resolvePropertyType(BeanWrapper beanWrapper, PersistentEntity beanClass, String propertyName) {
		Class propertyType = null
		if (beanClass) {
			propertyType = resolveDomainPropertyType(beanClass, propertyName)
		}
		if (!propertyType) {
			propertyType = resolveNonDomainPropertyType(beanWrapper, propertyName)
		}
		propertyType
	}

	private Class resolveDomainPropertyType(PersistentEntity beanClass, String propertyName) {
		def propertyNameWithoutIndex = stripIndex(propertyName)
		def persistentProperty = beanClass.getPropertyByName(propertyNameWithoutIndex)
		if (!persistentProperty && beanClass.isIdentityName(propertyNameWithoutIndex)) {
			persistentProperty = beanClass.identity
		}
		if (!persistentProperty) {
			return null
		}
		boolean isIndexed = propertyName =~ INDEXED_PROPERTY_PATTERN
		if (isIndexed) {
			if (persistentProperty instanceof Basic) {
				persistentProperty.componentType
			} else if (persistentProperty instanceof Association) {
				persistentProperty.associatedEntity.javaClass
			}
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
