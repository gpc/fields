package org.codehaus.groovy.grails.plugins.beanfields

import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.plugins.beanfields.taglib.FormFieldsTagLib
import spock.lang.Specification

@TestFor(FormFieldsTagLib)
class CommandPropertyAccessorSpec extends Specification {

	BeanPropertyAccessorFactory factory = new BeanPropertyAccessorFactory(grailsApplication: grailsApplication, applicationContext: applicationContext)

	TestCommand command

	void setup() {
		command = mockCommandObject(TestCommand)
	}

	void 'resolves a basic property'() {
		given:
		command.password = 'correct horse battery staple'

		and:
		def propertyAccessor = factory.accessorFor(command, 'password')

		expect:
		propertyAccessor.value == command.password
		propertyAccessor.rootBeanType == TestCommand
		propertyAccessor.beanType == TestCommand
		propertyAccessor.pathFromRoot == "password"
		propertyAccessor.propertyName == "password"
		propertyAccessor.propertyType == String
		!propertyAccessor.constraints.blank
		propertyAccessor.constraints.password
	}

	void 'resolves a basic property even when its value is null'() {
		given:
		def propertyAccessor = factory.accessorFor(command, 'password')

		expect:
		propertyAccessor.value == null
		propertyAccessor.rootBeanType == TestCommand
		propertyAccessor.beanType == TestCommand
		propertyAccessor.pathFromRoot == "password"
		propertyAccessor.propertyName == "password"
		propertyAccessor.propertyType == String
		!propertyAccessor.constraints.blank
		propertyAccessor.constraints.password
	}

	void 'resolves a simple indexed property'() {
		given:
		command.listOfStrings = ['correct', 'horse', 'battery', 'staple']

		and:
		def propertyAccessor = factory.accessorFor(command, 'listOfStrings[1]')

		expect:
		propertyAccessor.value == command.listOfStrings[1]
		propertyAccessor.rootBeanType == TestCommand
		propertyAccessor.beanType == TestCommand
		propertyAccessor.pathFromRoot == "listOfStrings[1]"
		propertyAccessor.propertyName == "listOfStrings"
		propertyAccessor.propertyType == String
	}

	void 'resolves a simple indexed property when the value at that index is null'() {
		given:
		command.listOfStrings = ['correct', null, 'battery', 'staple']

		and:
		def propertyAccessor = factory.accessorFor(command, 'listOfStrings[1]')

		expect:
		propertyAccessor.value == null
		propertyAccessor.pathFromRoot == "listOfStrings[1]"
		propertyAccessor.propertyName == "listOfStrings"
		propertyAccessor.propertyType == String
	}

	void 'resolves an untyped simple indexed property when the value at that index is null'() {
		given:
		command.untypedList = ['correct', null, 'battery', 'staple']

		and:
		def propertyAccessor = factory.accessorFor(command, 'untypedList[1]')

		expect:
		propertyAccessor.value == null
		propertyAccessor.pathFromRoot == "untypedList[1]"
		propertyAccessor.propertyName == "untypedList"
		propertyAccessor.propertyType == Object
	}

	void 'resolves a simple mapped property'() {
		given:
		def today = new Date()
		command.mapOfDates = [yesterday: today -1, today: today, tomorrow: today + 1]

		and:
		def propertyAccessor = factory.accessorFor(command, 'mapOfDates[today]')

		expect:
		propertyAccessor.value == command.mapOfDates['today']
		propertyAccessor.rootBeanType == TestCommand
		propertyAccessor.beanType == TestCommand
		propertyAccessor.pathFromRoot == "mapOfDates[today]"
		propertyAccessor.propertyName == "mapOfDates"
		propertyAccessor.propertyType == Date
	}

	void 'resolves a simple mapped property when the value at that index is null'() {
		given:
		def today = new Date()
		command.mapOfDates = [yesterday: today -1, today: null, tomorrow: today + 1]

		and:
		def propertyAccessor = factory.accessorFor(command, 'mapOfDates[today]')

		expect:
		propertyAccessor.value == null
		propertyAccessor.pathFromRoot == "mapOfDates[today]"
		propertyAccessor.propertyName == "mapOfDates"
		propertyAccessor.propertyType == Date
	}

	void 'resolves an untyped simple mapped property when the value at that index is null'() {
		given:
		def today = new Date()
		command.untypedMap = [yesterday: today -1, today: null, tomorrow: today + 1]

		and:
		def propertyAccessor = factory.accessorFor(command, 'untypedMap[today]')

		expect:
		propertyAccessor.value == null
		propertyAccessor.pathFromRoot == "untypedMap[today]"
		propertyAccessor.propertyName == "untypedMap"
		propertyAccessor.propertyType == Object
	}

	void 'resolves an enum property'() {
		given:
		command.gender = Gender.Male

		and:
		def propertyAccessor = factory.accessorFor(command, 'gender')

		expect:
		propertyAccessor.value == command.gender
		propertyAccessor.pathFromRoot == "gender"
		propertyAccessor.propertyName == "gender"
		propertyAccessor.propertyType == Gender
	}

	void 'resolves a nested property'() {
		given:
		command.address = new Address(street: '54 Evergreen Terrace', city: 'Springfield', country: 'USA')

		and:
		def propertyAccessor = factory.accessorFor(command, 'address.city')

		expect:
		propertyAccessor.value == command.address.city
		propertyAccessor.rootBeanType == TestCommand
		propertyAccessor.beanType == Address
		propertyAccessor.pathFromRoot == "address.city"
		propertyAccessor.propertyName == "city"
		propertyAccessor.propertyType == String
	}

	void 'resolves a nested property even when the intervening object is null'() {
		given:
		def propertyAccessor = factory.accessorFor(command, 'address.city')

		expect:
		propertyAccessor.value == null
		propertyAccessor.rootBeanType == TestCommand
		propertyAccessor.beanType == Address
		propertyAccessor.pathFromRoot == "address.city"
		propertyAccessor.propertyName == "city"
		propertyAccessor.propertyType == String
	}

	void 'if a nested property is a domain class then it is handled as one'() {
		given:
		command.address = new Address(street: '54 Evergreen Terrace', city: 'Springfield', country: 'USA')

		and:
		def propertyAccessor = factory.accessorFor(command, 'address.city')

		expect:
		propertyAccessor.persistentProperty
		propertyAccessor.persistentProperty.name == 'city'
	}

}

class TestCommand {

	String username
	String password
	List<String> listOfStrings
	List untypedList
	Map<String, Date> mapOfDates
	Map untypedMap
	Gender gender
	Address address

	static constraints = {
		username blank: false
		password blank: false, password: true
	}
}