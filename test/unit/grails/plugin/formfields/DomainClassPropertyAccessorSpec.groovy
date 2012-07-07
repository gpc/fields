package grails.plugin.formfields

import grails.test.mixin.web.ControllerUnitTestMixin
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.springframework.beans.NotReadablePropertyException
import grails.plugin.formfields.mock.*
import grails.test.mixin.*
import spock.lang.*

@TestMixin(ControllerUnitTestMixin)
@Mock([Person, Author, Book, Employee])
@Unroll
class DomainClassPropertyAccessorSpec extends Specification {

	BeanPropertyAccessorFactory factory = new BeanPropertyAccessorFactory(
		grailsApplication: grailsApplication,
		constraintsEvaluator: new DefaultConstraintEvaluator(),
		proxyHandler: new DefaultProxyHandler()
	)
	@Shared Address address
	@Shared Person person
	@Shared Employee employee
	@Shared Author author

	void setup() {
		address = new Address(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")
		person = new Person(name: "Bart Simpson", password: "bartman", gender: Gender.Male, dateOfBirth: new Date(87, 3, 19), address: address)
		person.emails = [home: "bart@thesimpsons.net", school: "bart.simpson@springfieldelementary.edu"]
		person.save(failOnError: true)

		employee = new Employee(name: 'Homer J Simpson', jobTitle: 'Safety officer', password: 'mmmdonuts', gender: Gender.Male, address: address)
		employee.save(failOnError: true)

		author = new Author(name: "William Gibson")
		author.addToBooks new Book(title: "Pattern Recognition")
		author.addToBooks new Book(title: "Spook Country")
		author.addToBooks new Book(title: "Zero History")
		author.save(failOnError: true)
	}

	void "fails sensibly when given an invalid property path"() {
		when:
		factory.accessorFor(person, "invalid")

		then:
		thrown NotReadablePropertyException
	}

	void "resolves basic property"() {
		given:
		def propertyAccessor = factory.accessorFor(person, "name")

		expect:
		propertyAccessor.value == person.name
		propertyAccessor.rootBeanType == Person
		propertyAccessor.rootBeanClass.clazz == Person // TODO: should we really be testing these non-interface fields?
		propertyAccessor.beanType == Person
		propertyAccessor.beanClass.clazz == Person
		propertyAccessor.pathFromRoot == "name"
		propertyAccessor.propertyName == "name"
		propertyAccessor.propertyType == String
		propertyAccessor.persistentProperty.name == "name"
	}

	void "resolves embedded property"() {
		given:
		def propertyAccessor = factory.accessorFor(person, "address.city")

		expect:
		propertyAccessor.value == address.city
		propertyAccessor.rootBeanType == Person
		propertyAccessor.rootBeanClass.clazz == Person
		propertyAccessor.beanType == Address
		propertyAccessor.beanClass == null // TODO: check if this is really the case when not mocked
		propertyAccessor.pathFromRoot == "address.city"
		propertyAccessor.propertyName == "city"
		propertyAccessor.propertyType == String
		propertyAccessor.persistentProperty == null
	}

	void "resolves property of indexed association"() {
		given:
		def propertyAccessor = factory.accessorFor(author, "books[0].title")

		expect:
		propertyAccessor.value == "Pattern Recognition"
		propertyAccessor.rootBeanType == Author
		propertyAccessor.rootBeanClass.clazz == Author
		propertyAccessor.beanType == Book
		propertyAccessor.beanClass.clazz == Book
		propertyAccessor.pathFromRoot == "books[0].title"
		propertyAccessor.propertyName == "title"
		propertyAccessor.propertyType == String
		propertyAccessor.persistentProperty.name == "title"
	}

	void "resolves other side of many-to-one association"() {
		given:
		def propertyAccessor = factory.accessorFor(author.books[0], "author.name")

		expect:
		propertyAccessor.value == author.name
		propertyAccessor.rootBeanType == Book
		propertyAccessor.rootBeanClass.clazz == Book
		propertyAccessor.beanType == Author
		propertyAccessor.beanClass.clazz == Author
		propertyAccessor.pathFromRoot == "author.name"
		propertyAccessor.propertyName == "name"
		propertyAccessor.propertyType == String
		propertyAccessor.persistentProperty.name == "name"
	}

	void "resolves property of simple mapped association"() {
		given:
		def propertyAccessor = factory.accessorFor(person, "emails[home]")

		expect:
		propertyAccessor.value == "bart@thesimpsons.net"
		propertyAccessor.rootBeanType == Person
		propertyAccessor.beanType == Person
		propertyAccessor.pathFromRoot == "emails[home]"
		propertyAccessor.propertyName == "emails"
		propertyAccessor.propertyType == String
		propertyAccessor.persistentProperty.name == "emails"
	}

	void "resolves basic property when value is null"() {
		given:
		person.name = null

		and:
		def propertyAccessor = factory.accessorFor(person, "name")

		expect:
		propertyAccessor.value == null
		propertyAccessor.pathFromRoot == "name"
		propertyAccessor.propertyName == "name"
		propertyAccessor.propertyType == String
		propertyAccessor.persistentProperty.name == "name"
	}

	void "resolves embedded property when intervening path is null"() {
		given:
		person.address = null

		and:
		def propertyAccessor = factory.accessorFor(person, "address.city")

		expect:
		propertyAccessor.value == null
		propertyAccessor.pathFromRoot == "address.city"
		propertyAccessor.propertyName == "city"
		propertyAccessor.propertyType == String
		propertyAccessor.persistentProperty == null
	}

	@Issue('https://github.com/robfletcher/grails-fields/issues/37')
	void "resolves constraints of the '#property' property when the intervening path is null"() {
		given:
		def book = new Book()

		and:
		def propertyAccessor = factory.accessorFor(book, property)

		expect:
		propertyAccessor.isRequired()          || !isRequired
		propertyAccessor.constraints?.nullable || isRequired
		propertyAccessor.constraints?.blank    || isRequired

		where:
		property              | isRequired
		'author.name'         | true
		'author.id'           | true
		'author.placeOfBirth' | false
	}

	void "resolves constraints of basic domain class property"() {
		given:
		def propertyAccessor = factory.accessorFor(person, "name")

		expect:
		!propertyAccessor.constraints.nullable
		!propertyAccessor.constraints.blank
	}

	void "type of '#property' is #type.name"() {
		given:
		def bean = beanType.list().first()
		def propertyAccessor = factory.accessorFor(bean, property)

		expect:
		propertyAccessor.propertyType == type

		where:
		beanType | property         | type
		Person   | "dateOfBirth"    | Date
		Person   | "address"        | Address
		Person   | "address.city"   | String
		Author   | "books"          | List
		Author   | "books[0]"       | Book
		Author   | "books[0].title" | String
	}

	void "resolves constraints of embedded property"() {
		given:
		def propertyAccessor = factory.accessorFor(person, "address.country")

		expect:
		!propertyAccessor.constraints.nullable
		propertyAccessor.constraints.inList == ["USA", "UK", "Canada"]
	}

	@Issue('https://github.com/robfletcher/grails-fields/issues/38')
	void "label keys for '#property' are '#labels'"() {
		given:
		def bean = beanType.list().find { it.class == beanType}
		def propertyAccessor = factory.accessorFor(bean, property)

		expect:
		propertyAccessor.labelKeys == labels

		where:
		beanType | property         | labels
		Person   | 'name'           | ['person.name.label']
		Person   | 'dateOfBirth'    | ['person.dateOfBirth.label']
		Person   | 'address'        | ['person.address.label']
		Person   | 'address.city'   | ['person.address.city.label', 'address.city.label']
		Author   | 'books[0].title' | ['author.books.title.label', 'book.title.label']
	}

	void "default label for '#property' is '#label'"() {
		given:
		def bean = beanType.list().first()
		def propertyAccessor = factory.accessorFor(bean, property)

		expect:
		propertyAccessor.defaultLabel == label

		where:
		beanType | property         | label
		Person   | "name"           | "Name"
		Person   | "dateOfBirth"    | "Date Of Birth"
		Person   | "address"        | "Address"
		Person   | "address.city"   | "City"
		Author   | "books[0].title" | "Title"
	}

	void "resolves errors for a basic property"() {
		given:
		person.name = ""

		and:
		def propertyAccessor = factory.accessorFor(person, "name")

		expect:
		!person.validate()

		and:
		propertyAccessor.errors.first().code == "blank"
		propertyAccessor.invalid
	}

	@Issue("http://jira.grails.org/browse/GRAILS-7713")
	void "resolves errors for an embedded property"() {
		given:
		person.address.country = "Australia"
		person.errors.rejectValue('address.country', 'not.inList') // http://jira.grails.org/browse/GRAILS-8480

		and:
		def propertyAccessor = factory.accessorFor(person, "address.country")

		expect:
		propertyAccessor.errors.first().code == "not.inList"
		propertyAccessor.invalid
	}

	@Issue("http://jira.grails.org/browse/GRAILS-7713")
	void "resolves errors for an indexed property"() {
		given:
		author.books[0].title = ""
		author.errors.rejectValue('books[0].title', 'blank') // http://jira.grails.org/browse/GRAILS-7713

		and:
		def propertyAccessor = factory.accessorFor(author, "books[0].title")

		expect:
		propertyAccessor.errors.first().code == "blank"
		propertyAccessor.invalid
	}

	void "the #path property is required:#expected"() {
		given:
		def propertyAccessor = factory.accessorFor(person, path)

		expect:
		propertyAccessor.required == expected

		where:
		path          | expected
		"name"        | true // non-blank string
		"dateOfBirth" | false // nullable object
		"password"    | false // blank string
		"gender"      | true // non-nullable string
		"minor"       | false // boolean properties are never considered required
	}

	def 'the superclasses of #type.simpleName are #expected'() {
		given:
		def propertyAccessor = factory.accessorFor(type.newInstance(), path)

		expect:
		propertyAccessor.beanSuperclasses == expected

		where:
		type     | path   | expected
		Person   | 'name' | []
		Employee | 'name' | [Person]
	}

	void 'the superclasses of Person.#path are #expected'() {
		given:
		def propertyAccessor = factory.accessorFor(person, path)

		expect:
		propertyAccessor.propertyTypeSuperclasses == expected

		where:
		path          | expected
		'name'        | [CharSequence]
		'gender'      | [Enum]
		'dateOfBirth' | []
	}

	void 'correctly reports association status of Author.#path'() {
		given:
		def propertyAccessor = factory.accessorFor(author, path)

		expect:
		propertyAccessor.association ^ !isAssociation

		where:
		path              | isAssociation
		'name'            | false
		'books'           | true
		'books[0].title'  | false
		'books[0].author' | true
	}

}
