package grails.plugin.formfields

import grails.test.mixin.web.GroovyPageUnitTestMixin
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import grails.plugin.formfields.mock.*
import grails.test.mixin.*
import spock.lang.*

@Issue('https://github.com/robfletcher/grails-fields/issues/39')
@TestMixin(GroovyPageUnitTestMixin)
@TestFor(FormFieldsTemplateService)
@Mock([Book, Author])
class AssociationTypeTemplatesSpec extends Specification {

	def factory = new BeanPropertyAccessorFactory()
	Author authorInstance

	void setup() {
		factory.grailsApplication = grailsApplication
		factory.constraintsEvaluator = new DefaultConstraintEvaluator()
		factory.proxyHandler = new DefaultProxyHandler()

		authorInstance = new Author(name: 'William Gibson')
		authorInstance.addToBooks new Book(title: 'Pattern Recognition')
		authorInstance.addToBooks new Book(title: 'Spook Country')
		authorInstance.addToBooks new Book(title: 'Zero History')
		authorInstance.save(failOnError: true, flush: true)
	}

	void 'resolves template for association type'() {
		given:
		views['/_fields/default/_field.gsp'] = 'DEFAULT FIELD TEMPLATE'
		views['/_fields/list/_field.gsp'] = 'PROPERTY TYPE TEMPLATE'
		views['/_fields/oneToMany/_field.gsp'] = 'ASSOCIATION TYPE TEMPLATE'

		and:
		def property = factory.accessorFor(authorInstance, 'books')

		expect:
		def template = service.findTemplate(property, 'field')
		render(template: template.path) == 'ASSOCIATION TYPE TEMPLATE'
	}

	void 'property name trumps association type'() {
		given:
		views['/_fields/default/_field.gsp'] = 'DEFAULT FIELD TEMPLATE'
		views['/_fields/list/_field.gsp'] = 'PROPERTY TYPE TEMPLATE'
		views['/_fields/oneToMany/_field.gsp'] = 'ASSOCIATION TYPE TEMPLATE'
		views['/_fields/author/books/_field.gsp'] = 'PROPERTY NAME TEMPLATE'

		and:
		def property = factory.accessorFor(authorInstance, 'books')

		expect:
		def template = service.findTemplate(property, 'field')
		render(template: template.path) == 'PROPERTY NAME TEMPLATE'
	}

}
