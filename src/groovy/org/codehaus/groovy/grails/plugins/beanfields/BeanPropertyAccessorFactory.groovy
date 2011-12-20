package org.codehaus.groovy.grails.plugins.beanfields

import java.lang.reflect.ParameterizedType
import java.util.regex.Pattern
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.validation.*
import org.springframework.beans.*

class BeanPropertyAccessorFactory implements GrailsApplicationAware {

	GrailsApplication grailsApplication
	ConstraintsEvaluator constraintsEvaluator

	BeanPropertyAccessor accessorFor(bean, String propertyPath) {
		def params = [rootBean: bean, rootBeanType: bean.getClass(), pathFromRoot: propertyPath]
		params.rootBeanClass = resolveDomainClass(bean.getClass())

		resolvePropertyFromPath(bean, propertyPath, params)

		new UnifiedBeanPropertyAccessor(params)
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
		println "wrappedClass: $beanWrapper.wrappedClass, beanClass: $beanClass"
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
			beanClass.constrainedProperties[propertyName]
		} else {
			constraintsEvaluator.evaluate(beanWrapper.wrappedClass)[propertyName]
		}
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

//	private Class resolveDomainPropertyType(GrailsDomainClass beanClass, String propertyName) {
//		def persistentProperty = beanClass.getPersistentProperty(stripIndex(propertyName))
//		if (persistentProperty.embedded) {
//			persistentProperty.component.clazz
//		} else if (persistentProperty.association) {
//			persistentProperty.referencedDomainClass.clazz
//		} else {
//			null
//		}
//	}

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
		value ? PropertyAccessorFactory.forBeanPropertyAccess(value) : new BeanWrapperImpl(type)
	}

	private static final Pattern INDEXED_PROPERTY_PATTERN = ~/^(\w+)\[(.+)\]$/

	private final String stripIndex(String propertyName) {
		def matcher = propertyName =~ INDEXED_PROPERTY_PATTERN
		matcher.matches() ? matcher[0][1] : propertyName
	}
}
