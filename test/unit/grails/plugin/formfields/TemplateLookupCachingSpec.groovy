package grails.plugin.formfields

import grails.plugin.formfields.mock.Person
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.springframework.core.io.ByteArrayResource
import org.codehaus.groovy.grails.web.pages.discovery.*
import spock.lang.*

@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/5')
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
		beanPropertyAccessorFactory.proxyHandler = new DefaultProxyHandler()
	}

	def setup() {
		service.groovyPageLocator = mockGroovyPageLocator
	}

	void 'a template is looked up the first time it is required'() {
		given:
		def templateResource = new GroovyPageResourceScriptSource('/_fields/person/name/_widget.gsp', new ByteArrayResource('PERSON NAME TEMPLATE'.getBytes('UTF-8')))

		and:
		def property = beanPropertyAccessorFactory.accessorFor(person, 'name')

		when:
		def template = service.findTemplate(property, 'input', null)

		then:
		template.path == '/_fields/person/name/input'

		and:
		1 * mockGroovyPageLocator.findTemplateByPath(_) >> templateResource
	}

	void 'the next time the template is cached'() {
		given:
		def property = beanPropertyAccessorFactory.accessorFor(person, 'name')

		when:
		def template = service.findTemplate(property, 'input', null)

		then:
		template.path == '/_fields/person/name/input'

		and:
		0 * mockGroovyPageLocator.findTemplateByPath(_)
	}

	void 'a template for a different property is cached separately'() {
		given:
		def templateResource = new GroovyPageResourceScriptSource('/_fields/person/password/_widget.gsp', new ByteArrayResource('PERSON PASSWORD TEMPLATE'.getBytes('UTF-8')))

		and:
		def property = beanPropertyAccessorFactory.accessorFor(person, 'password')

		when:
		def template = service.findTemplate(property, 'input', null)

		then:
		template.path == '/_fields/person/password/input'

		and:
		1 * mockGroovyPageLocator.findTemplateByPath(_) >> templateResource
	}

	void 'a different template for the same property is cached separately'() {
		given:
		def templateResource = new GroovyPageResourceScriptSource('/_fields/person/name/_widget.gsp', new ByteArrayResource('PERSON NAME TEMPLATE 2'.getBytes('UTF-8')))

		and:
		def property = beanPropertyAccessorFactory.accessorFor(person, 'name')
		
		when:
		def template = service.findTemplate(property, 'field', null)
		
		then:
		template.path == '/_fields/person/name/field'

		and:
		1 * mockGroovyPageLocator.findTemplateByPath(_) >> templateResource
	}

}
