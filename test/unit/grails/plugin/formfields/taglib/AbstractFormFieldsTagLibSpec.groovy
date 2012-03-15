package grails.plugin.formfields.taglib

import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import grails.plugin.formfields.*
import grails.plugin.formfields.mock.*
import grails.test.mixin.*
import spock.lang.*

abstract class AbstractFormFieldsTagLibSpec extends Specification {

	def personInstance

	def setup() {
		personInstance = new Person(name: "Bart Simpson", password: "bartman", gender: Gender.Male, dateOfBirth: new Date(87, 3, 19), minor: true)
		personInstance.address = new Address(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")
	}

	def cleanup() {
		views.clear()
		applicationContext.getBean("groovyPagesTemplateEngine").clearPageCache()
		applicationContext.getBean("groovyPagesTemplateRenderer").clearCache()

		messageSource.@messages.clear() // bit of a hack but messages don't get torn down otherwise
	}

}
