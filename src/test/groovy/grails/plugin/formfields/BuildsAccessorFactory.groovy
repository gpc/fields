package grails.plugin.formfields

import grails.core.GrailsApplication
import grails.core.support.proxy.DefaultProxyHandler
import org.grails.scaffolding.model.property.DomainPropertyFactoryImpl
import org.grails.validation.DefaultConstraintEvaluator

/**
 * Created by jameskleeh on 5/3/17.
 */
trait BuildsAccessorFactory {

    BeanPropertyAccessorFactory buildFactory(GrailsApplication grailsApplication) {
        BeanPropertyAccessorFactory factory = new BeanPropertyAccessorFactory()
        factory.grailsApplication = grailsApplication
        factory.constraintsEvaluator = new DefaultConstraintEvaluator()
        factory.proxyHandler = new DefaultProxyHandler()
        factory.grailsDomainClassMappingContext = new MappingContextBuilder(grailsApplication).build()
        factory.fieldsDomainPropertyFactory = new DomainPropertyFactoryImpl(grailsDomainClassMappingContext: factory.grailsDomainClassMappingContext, trimStrings: true, convertEmptyStringsToNull: true)
        factory
    }
}