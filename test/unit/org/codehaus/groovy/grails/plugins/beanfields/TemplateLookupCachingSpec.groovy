package org.codehaus.groovy.grails.plugins.beanfields

import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.plugins.beanfields.mock.Person
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.springframework.core.io.ByteArrayResource
import org.codehaus.groovy.grails.web.pages.discovery.*
import spock.lang.*

@Issue('https://github.com/robfletcher/grails-form-fields/issues/5')
@TestFor(FormFieldsTemplateService)
@Stepwise
class TemplateLookupCachingSpec extends Specification {

	def mockGroovyPageLocator = Mock(GrailsConventionGroovyPageLocator)
	@Shared def beanPropertyAccessorFactory = new BeanPropertyAccessorFactory()

	def setupSpec() {
		beanPropertyAccessorFactory.grailsApplication = grailsApplication
		beanPropertyAccessorFactory.constraintsEvaluator = new DefaultConstraintEvaluator()
	}

	def setup() {
		service.groovyPageLocator = mockGroovyPageLocator
	}

	void 'a template is looked up the first time it is required'() {
		given:
		def templateResource = new GroovyPageResourceScriptSource('/forms/person/name/_input.gsp', new ByteArrayResource('THE TEMPLATE'.getBytes('UTF-8')))

		and:
		def person = new Person(name: 'Bart Simpson')
		def property = beanPropertyAccessorFactory.accessorFor(person, 'name')

		when:
		def template = service.findTemplate(property, 'input')

		then:
		template.path == '/forms/person/name/input'
		template.source.scriptAsString == 'THE TEMPLATE'

		and:
		1 * mockGroovyPageLocator.findTemplateByPath(_) >> templateResource
	}

	void 'the next time the template is cached'() {
		given:
		def person = new Person(name: 'Bart Simpson')
		def property = beanPropertyAccessorFactory.accessorFor(person, 'name')

		when:
		def template = service.findTemplate(property, 'input')

		then:
		template.path == '/forms/person/name/input'
		template.source.scriptAsString == 'THE TEMPLATE'

		and:
		0 * mockGroovyPageLocator.findTemplateByPath(_)
	}

}
