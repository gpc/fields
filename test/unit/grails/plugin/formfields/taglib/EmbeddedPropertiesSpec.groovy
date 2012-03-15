package grails.plugin.formfields.taglib

import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import grails.plugin.formfields.*
import grails.plugin.formfields.mock.*
import grails.test.mixin.*
import spock.lang.*

@Issue('https://github.com/robfletcher/grails-fields/issues/30')
@TestFor(FormFieldsTagLib)
@Mock(Person)
class EmbeddedPropertiesSpec extends AbstractFormFieldsTagLibSpec {

	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

	def setupSpec() {
		configurePropertyAccessorSpringBean()
	}

	def setup() {
		def taglib = applicationContext.getBean(FormFieldsTagLib)

		mockFormFieldsTemplateService.findTemplate(_, 'field') >> [path: '/_fields/default/field']
		taglib.formFieldsTemplateService = mockFormFieldsTemplateService

		mockEmbeddedSitemeshLayout(taglib)
	}

	void "field tag renders individual fields for embedded properties"() {
		given:
		views["/_fields/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:field bean="personInstance" property="address"/>', [personInstance: personInstance])

		then:
		output.contains('address.street address.city address.country')
	}

	void "field tag wraps embedded properties in a container"() {
		given:
		views["/_fields/default/_field.gsp"] = '${property} '

		expect:
		applyTemplate('<f:field bean="personInstance" property="address"/>', [personInstance: personInstance]) == '<fieldset class="embedded address"><legend>Address</legend>address.street address.city address.country </fieldset>'
	}

	void "embedded property label is resolved from message bundle"() {
		given:
		views["/_fields/default/_field.gsp"] = '${property} '

		and:
		messageSource.addMessage('person.address.label', request.locale, 'Address of person')

		when:
		def output = applyTemplate('<f:field bean="personInstance" property="address"/>', [personInstance: personInstance])

		then:
		output.contains('<legend>Address of person</legend>')
	}

}