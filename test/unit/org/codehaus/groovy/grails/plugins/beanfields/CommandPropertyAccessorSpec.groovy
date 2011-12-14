package org.codehaus.groovy.grails.plugins.beanfields

import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.plugins.beanfields.taglib.FormFieldsTagLib
import spock.lang.Specification

@TestFor(FormFieldsTagLib)
class CommandPropertyAccessorSpec extends Specification {

	BeanPropertyAccessorFactory factory = new BeanPropertyAccessorFactory(grailsApplication: grailsApplication)

	void 'resolves properties of a command object'() {
		given:
		def command = mockCommandObject(LoginCommand)
		command.password = 'correct horse battery staple'

		and:
		def propertyAccessor = factory.accessorFor(command, 'password')

		expect:
		propertyAccessor.value == command.password
		propertyAccessor.rootBeanType == LoginCommand
		propertyAccessor.rootBeanClass == null
		propertyAccessor.beanType == LoginCommand
		propertyAccessor.beanClass == null
		propertyAccessor.pathFromRoot == "password"
		propertyAccessor.propertyName == "password"
		propertyAccessor.type == String
		propertyAccessor.persistentProperty == null
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