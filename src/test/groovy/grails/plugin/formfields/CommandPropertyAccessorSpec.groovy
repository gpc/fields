package grails.plugin.formfields

import grails.core.support.proxy.DefaultProxyHandler
import grails.test.mixin.web.ControllerUnitTestMixin
import grails.plugin.formfields.mock.*
import grails.test.mixin.*
import org.grails.validation.DefaultConstraintEvaluator
import spock.lang.*

@TestMixin(ControllerUnitTestMixin)
@Mock(Person)
@Unroll
class CommandPropertyAccessorSpec extends Specification {

	BeanPropertyAccessorFactory factory = new BeanPropertyAccessorFactory(
			grailsApplication: grailsApplication,
			constraintsEvaluator: new DefaultConstraintEvaluator(),
			proxyHandler: new DefaultProxyHandler()
	)

	void 'resolves a basic property'() {
		given:
		TestCommand command = mockCommandObject(TestCommand)
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
		!propertyAccessor.invalid
		propertyAccessor.required
		!propertyAccessor.constraints.blank
		propertyAccessor.constraints.password
	}

	void 'property of #type.simpleName is nullable'() {
		given:
		def command = mockCommandObject(type)

		and:
		def propertyAccessor = factory.accessorFor(command, 'stringProperty')

		expect:
		propertyAccessor.constraints.nullable
		!propertyAccessor.required
		!propertyAccessor.invalid

		where:
		type << [TestCommand, UnconstrainedCommand]
	}

	void 'resolves a basic property even when its value is null'() {
		given:
		TestCommand command = mockCommandObject(TestCommand)

		and:
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
		TestCommand command = mockCommandObject(TestCommand)
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
		TestCommand command = mockCommandObject(TestCommand)
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
		TestCommand command = mockCommandObject(TestCommand)
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
		TestCommand command = mockCommandObject(TestCommand)
		def today = new Date()
		command.mapOfDates = [yesterday: today - 1, today: today, tomorrow: today + 1]

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
		TestCommand command = mockCommandObject(TestCommand)
		def today = new Date()
		command.mapOfDates = [yesterday: today - 1, today: null, tomorrow: today + 1]

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
		TestCommand command = mockCommandObject(TestCommand)
		def today = new Date()
		command.untypedMap = [yesterday: today - 1, today: null, tomorrow: today + 1]

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
		TestCommand command = mockCommandObject(TestCommand)
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
		TestCommand command = mockCommandObject(TestCommand)
		command.person = new Person(name: 'Bart Simpson')

		and:
		def propertyAccessor = factory.accessorFor(command, 'person.name')

		expect:
		propertyAccessor.value == command.person.name
		propertyAccessor.rootBeanType == TestCommand
		propertyAccessor.beanType == Person
		propertyAccessor.pathFromRoot == "person.name"
		propertyAccessor.propertyName == "name"
		propertyAccessor.propertyType == String
	}

	void 'resolves a nested property even when the intervening object is null'() {
		given:
		TestCommand command = mockCommandObject(TestCommand)

		and:
		def propertyAccessor = factory.accessorFor(command, 'person.name')

		expect:
		propertyAccessor.value == null
		propertyAccessor.rootBeanType == TestCommand
		propertyAccessor.beanType == Person
		propertyAccessor.pathFromRoot == "person.name"
		propertyAccessor.propertyName == "name"
		propertyAccessor.propertyType == String
	}

	@Issue('https://github.com/grails-fields-plugin/grails-fields/issues/37')
	void "resolves constraints of the '#property' property even when the intervening object is null"() {
		given:
		TestCommand command = mockCommandObject(TestCommand)

		and:
		def propertyAccessor = factory.accessorFor(command, property)

		expect:
		propertyAccessor.isRequired() || !isRequired
		propertyAccessor.constraints?.nullable || isRequired
		propertyAccessor.constraints?.blank || isRequired

		where:
		property             | isRequired
		'person.name'        | true
		'person.id'          | true
		'person.dateOfBirth' | false
	}

	void 'if a nested property is a domain class then it is handled as one'() {
		given:
		TestCommand command = mockCommandObject(TestCommand)
		command.person = new Person(name: 'Bart Simpson')

		and:
		def propertyAccessor = factory.accessorFor(command, 'person.name')

		expect:
		propertyAccessor.persistentProperty
		propertyAccessor.persistentProperty.name == 'name'
	}

	void 'constraints are defaulted for classes that have no constraints property'() {
		given:
		UnconstrainedCommand command = mockCommandObject(UnconstrainedCommand)

		and:
		def propertyAccessor = factory.accessorFor(command, 'stringProperty')

		expect:
		propertyAccessor.constraints
		propertyAccessor.constraints.nullable
		propertyAccessor.constraints.blank
	}

}

class TestCommand {

	String username
	String password
	String stringProperty
	List<String> listOfStrings
	List untypedList
	Map<String, Date> mapOfDates
	Map untypedMap
	Gender gender
	Person person

	static constraints = {
		username blank: false
		password blank: false, password: true
	}
}

class UnconstrainedCommand {
	String stringProperty
}