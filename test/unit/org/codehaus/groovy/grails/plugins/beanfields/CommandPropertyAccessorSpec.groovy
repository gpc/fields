package org.codehaus.groovy.grails.plugins.beanfields

import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.plugins.beanfields.taglib.FormFieldsTagLib
import spock.lang.Specification

@TestFor(FormFieldsTagLib)
class CommandPropertyAccessorSpec extends Specification {

	BeanPropertyAccessorFactory factory = new BeanPropertyAccessorFactory(grailsApplication: grailsApplication, applicationContext: applicationContext)

	void 'resolves properties of a command object'() {
		given:
		def command = mockCommandObject(LoginCommand)
		command.password = 'correct horse battery staple'

		and:
		def propertyAccessor = factory.accessorFor(command, 'password')

		expect:
		propertyAccessor.value == command.password
		propertyAccessor.rootBeanType == LoginCommand
		propertyAccessor.beanType == LoginCommand
		propertyAccessor.pathFromRoot == "password"
		propertyAccessor.propertyName == "password"
		propertyAccessor.type == String
		!propertyAccessor.constraints.blank
		propertyAccessor.constraints.password
	}

}

class LoginCommand {
	String username
	String password
	static constraints = {
		username blank: false
		password blank: false, password: true
	}
}