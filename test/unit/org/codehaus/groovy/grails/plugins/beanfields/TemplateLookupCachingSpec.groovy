package org.codehaus.groovy.grails.plugins.beanfields

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.codehaus.groovy.grails.plugins.beanfields.mock.Person
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.springframework.core.io.ByteArrayResource
import org.codehaus.groovy.grails.web.pages.discovery.*
import spock.lang.*

@Issue('https://github.com/robfletcher/grails-form-fields/issues/5')
@TestMixin(GrailsUnitTestMixin)
@Stepwise
class TemplateLookupCachingSpec extends Specification {

	@Shared def service = new FormFieldsTemplateService()
	def mockGroovyPageLocator = Mock(GrailsConventionGroovyPageLocator)
	@Shared def beanPropertyAccessorFactory = new BeanPropertyAccessorFactory()
	def person = new Person(name: 'Bart Simpson', password: 'eatmyshorts')

	def setupSpec() {
		service.pluginManager = applicationContext.pluginManager

		beanPropertyAccessorFactory.grailsApplication = grailsApplication
		beanPropertyAccessorFactory.constraintsEvaluator = new DefaultConstraintEvaluator()
	}

	def setup() {
		service.groovyPageLocator = mockGroovyPageLocator
	}

	void 'a template is looked up the first time it is required'() {
		given:
		def templateResource = new GroovyPageResourceScriptSource('/forms/person/name/_input.gsp', new ByteArrayResource('PERSON NAME TEMPLATE'.getBytes('UTF-8')))

		and:
		def property = beanPropertyAccessorFactory.accessorFor(person, 'name')

		when:
		def template = service.findTemplate(property, 'input')

		then:
		template.path == '/forms/person/name/input'
		template.source.scriptAsString == 'PERSON NAME TEMPLATE'

		and:
		1 * mockGroovyPageLocator.findTemplateByPath(_) >> templateResource
	}

	void 'the next time the template is cached'() {
		given:
		def property = beanPropertyAccessorFactory.accessorFor(person, 'name')

		when:
		def template = service.findTemplate(property, 'input')

		then:
		template.path == '/forms/person/name/input'
		template.source.scriptAsString == 'PERSON NAME TEMPLATE'

		and:
		0 * mockGroovyPageLocator.findTemplateByPath(_)
	}

	void 'a template for a different property is cached separately'() {
		given:
		def templateResource = new GroovyPageResourceScriptSource('/forms/person/password/_input.gsp', new ByteArrayResource('PERSON PASSWORD TEMPLATE'.getBytes('UTF-8')))

		and:
		def property = beanPropertyAccessorFactory.accessorFor(person, 'password')

		when:
		def template = service.findTemplate(property, 'input')

		then:
		template.path == '/forms/person/password/input'
		template.source.scriptAsString == 'PERSON PASSWORD TEMPLATE'

		and:
		1 * mockGroovyPageLocator.findTemplateByPath(_) >> templateResource
	}

	void 'a different template for the same property is cached separately'() {
		given:
		def templateResource = new GroovyPageResourceScriptSource('/forms/person/name/_input.gsp', new ByteArrayResource('PERSON NAME TEMPLATE 2'.getBytes('UTF-8')))

		and:
		def property = beanPropertyAccessorFactory.accessorFor(person, 'name')
		
		when:
		def template = service.findTemplate(property, 'field')
		
		then:
		template.path == '/forms/person/name/field'
		template.source.scriptAsString == 'PERSON NAME TEMPLATE 2'

		and:
		1 * mockGroovyPageLocator.findTemplateByPath(_) >> templateResource
	}

}
