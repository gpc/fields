package grails.plugin.formfields.taglib

import grails.core.support.proxy.DefaultProxyHandler
import grails.plugin.formfields.BeanPropertyAccessorFactory
import grails.plugin.formfields.MappingContextBuilder
import grails.plugin.formfields.MappingContextBuilderFactoryBean
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.plugins.web.DefaultGrailsTagDateHelper
import org.grails.scaffolding.model.property.DomainPropertyFactory
import org.grails.scaffolding.model.property.DomainPropertyFactoryImpl
import org.grails.validation.DefaultConstraintEvaluator
import org.grails.validation.GrailsDomainClassValidator
import spock.lang.Specification
import grails.plugin.formfields.mock.*

abstract class AbstractFormFieldsTagLibSpec extends Specification {

	Person personInstance
    Product productInstance

	def setup() {
		personInstance = new Person(name: "Bart Simpson", password: "bartman", gender: Gender.Male, dateOfBirth: new Date(87, 3, 19), minor: true)
		personInstance.address = new Address(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")

        productInstance = new Product(netPrice: 12.33)
	}

	def cleanup() {
		views.clear()
		applicationContext.getBean("groovyPagesTemplateEngine").clearPageCache()
		applicationContext.getBean("groovyPagesTemplateRenderer").clearCache()

		messageSource.@messages.clear() // bit of a hack but messages don't get torn down otherwise
	}
	
	protected void configurePropertyAccessorSpringBean() {
		defineBeans {
			grailsTagDateHelper(DefaultGrailsTagDateHelper)
			constraintsEvaluator(DefaultConstraintEvaluator)
			fieldsDomainPropertyFactory(DomainPropertyFactoryImpl)
			beanPropertyAccessorFactory(BeanPropertyAccessorFactory) {
				constraintsEvaluator = ref('constraintsEvaluator')
				proxyHandler = new DefaultProxyHandler()
				grailsDomainClassMappingContext = ref("grailsDomainClassMappingContext")
				fieldsDomainPropertyFactory = ref("fieldsDomainPropertyFactory")
			}
			grailsDomainClassMappingContext(MappingContextBuilderFactoryBean) {
				domains = [Person, Product, Author, Book] as Class[]
			}
		}
	}
	
	protected void mockEmbeddedSitemeshLayout(taglib) {
	 	taglib.metaClass.applyLayout = { Map attrs, Closure body ->
	 		if (attrs.name == '_fields/embedded') {
	 			out << '<fieldset class="embedded ' << attrs.params.type << '">'
	 			out << '<legend>' << attrs.params.legend << '</legend>'
	 			out << body()
	 			out << '</fieldset>'
	 		}
	 		null // stops default return
	 	}
	}

}
