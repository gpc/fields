package org.codehaus.groovy.grails.plugins.beanfields

import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.commons.*
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator

class BeanPropertyAccessorFactory implements GrailsApplicationAware, ApplicationContextAware {

	GrailsApplication grailsApplication
	ApplicationContext applicationContext

	BeanPropertyAccessor accessorFor(bean, String propertyPath) {
		def rootBeanClass = resolveDomainClass(bean.getClass())
		rootBeanClass ? propertyAccessorForDomainClassInstance(rootBeanClass, bean, propertyPath) : propertyAccessorForCommandClassInstance(bean, propertyPath)
	}

	private DomainClassPropertyAccessor propertyAccessorForDomainClassInstance(GrailsDomainClass rootBeanClass, bean, String propertyPath) {
		return new DomainClassPropertyAccessor(rootBeanClass, bean, propertyPath)
	}

	private CommandPropertyAccessor propertyAccessorForCommandClassInstance(bean, String propertyPath) {
		ConstraintsEvaluator constraintsEvaluator
		if (applicationContext.containsBean(ConstraintsEvaluator.BEAN_NAME)) {
			constraintsEvaluator = applicationContext.getBean(ConstraintsEvaluator.BEAN_NAME)
		} else {
			constraintsEvaluator = new DefaultConstraintEvaluator()
		}
		return new CommandPropertyAccessor(bean, propertyPath, constraintsEvaluator)
	}

	private GrailsDomainClass resolveDomainClass(Class beanClass) {
		grailsApplication.getDomainClass(beanClass.name)
	}

}
