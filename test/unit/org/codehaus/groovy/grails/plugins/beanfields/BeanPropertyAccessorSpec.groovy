package org.codehaus.groovy.grails.plugins.beanfields

import grails.persistence.Entity
import org.codehaus.groovy.grails.plugins.beanfields.taglib.FormFieldsTagLib
import org.springframework.beans.NotReadablePropertyException
import grails.test.mixin.*
import spock.lang.*

@TestFor(FormFieldsTagLib)
@Mock([Person, Address, Author, Book])
class BeanPropertyAccessorSpec extends Specification {

	BeanPropertyAccessorFactory factory = new BeanPropertyAccessorFactory(grailsApplication: grailsApplication)
	@Shared def address
	@Shared def person
	@Shared def author

	def setup() {
		address = new Address(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")
		person = new Person(name: "Bart Simpson", password: "bartman", gender: "Male", dateOfBirth: new Date(87, 3, 19), address: address, emails: [home: "bart@thesimpsons.net", school: "bart.simpson@springfieldelementary.edu"])
		person.save(failOnError: true)

		author = new Author(name: "William Gibson")
		author.addToBooks new Book(title: "Pattern Recognition")
		author.addToBooks new Book(title: "Spook Country")
		author.addToBooks new Book(title: "Zero History")
		author.save(failOnError: true)
	}

	def "fails sensibly when given an invalid property path"() {
		when:
		factory.accessorFor(person, "invalid")

		then:
		thrown NotReadablePropertyException
	}

	def "resolves basic property"() {
		given:
		def propertyAccessor = factory.accessorFor(person, "name")

		expect:
		propertyAccessor.value == person.name
		propertyAccessor.rootBeanType == Person
		propertyAccessor.rootBeanClass.clazz == Person
		propertyAccessor.beanType == Person
		propertyAccessor.beanClass.clazz == Person
		propertyAccessor.pathFromRoot == "name"
		propertyAccessor.propertyName == "name"
		propertyAccessor.type == String
		propertyAccessor.persistentProperty.name == "name"
	}

	def "resolves embedded property"() {
		given:
		def propertyAccessor = factory.accessorFor(person, "address.city")

		expect:
		propertyAccessor.value == address.city
		propertyAccessor.rootBeanType == Person
		propertyAccessor.rootBeanClass.clazz == Person
		propertyAccessor.beanType == Address
		propertyAccessor.beanClass.clazz == Address
		propertyAccessor.pathFromRoot == "address.city"
		propertyAccessor.propertyName == "city"
		propertyAccessor.type == String
		propertyAccessor.persistentProperty.name == "city"
	}

	def "resolves property of indexed association"() {
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
		propertyAccessor.type == String
		propertyAccessor.persistentProperty.name == "title"
	}

	def "resolves other side of many-to-one association"() {
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
		propertyAccessor.type == String
		propertyAccessor.persistentProperty.name == "name"
	}

	def "resolves property of simple mapped association"() {
		given:
		def propertyAccessor = factory.accessorFor(person, "emails[home]")

		expect:
		propertyAccessor.value == "bart@thesimpsons.net"
		propertyAccessor.rootBeanType == Person
		propertyAccessor.beanType == Person
		propertyAccessor.pathFromRoot == "emails[home]"
		propertyAccessor.propertyName == "emails"
		propertyAccessor.type == String
		propertyAccessor.persistentProperty.name == "emails"
	}

	def "resolves basic property when value is null"() {
		given:
		person.name = null

		and:
		def propertyAccessor = factory.accessorFor(person, "name")

		expect:
		propertyAccessor.value == null
		propertyAccessor.pathFromRoot == "name"
		propertyAccessor.propertyName == "name"
		propertyAccessor.type == String
		propertyAccessor.persistentProperty.name == "name"
	}

	def "resolves embedded property when intervening path is null"() {
		given:
		person.address = null

		and:
		def propertyAccessor = factory.accessorFor(person, "address.city")

		expect:
		propertyAccessor.value == null
		propertyAccessor.pathFromRoot == "address.city"
		propertyAccessor.propertyName == "city"
		propertyAccessor.type == String
		propertyAccessor.persistentProperty.name == "city"
	}

	def "resolves constraints of basic domain class property"() {
		given:
		def propertyAccessor = factory.accessorFor(person, "name")

		expect:
		!propertyAccessor.constraints.nullable
		!propertyAccessor.constraints.blank
	}

	@Unroll({ "type of '$property' is $type.name" })
	def "resolves type of property"() {
		given:
		def propertyAccessor = factory.accessorFor(bean, property)

		expect:
		propertyAccessor.type == type

		where:
		bean   | property         | type
		person | "dateOfBirth"    | Date
		person | "address"        | Address
		person | "address.city"   | String
		author | "books"          | List
		author | "books[0]"       | Book
		author | "books[0].title" | String
	}

	def "resolves constraints of embedded property"() {
		given:
		def propertyAccessor = factory.accessorFor(person, "address.country")

		expect:
		!propertyAccessor.constraints.nullable
		propertyAccessor.constraints.inList == ["USA", "UK", "Canada"]
	}

	@Unroll({ "label key for '$property' is '$label'" })
	def "label key is the same as the scaffolding convention"() {
		given:
		def propertyAccessor = factory.accessorFor(bean, property)

		expect:
		propertyAccessor.labelKey == label

		where:
		bean   | property         | label
		person | "name"           | "Person.name.label"
		person | "address"        | "Person.address.label"
		person | "address.city"   | "Address.city.label"
		author | "books[0].title" | "Book.title.label"
	}

	@Unroll({ "default label for '$property' is '$label'" })
	def "default label is the property's natural name"() {
		given:
		def propertyAccessor = factory.accessorFor(bean, property)

		expect:
		propertyAccessor.defaultLabel == label

		where:
		bean   | property         | label
		person | "name"           | "Name"
		person | "dateOfBirth"    | "Date Of Birth"
		person | "address"        | "Address"
		person | "address.city"   | "City"
		author | "books[0].title" | "Title"
	}

	def "resolves errors for a basic property"() {
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
	def "resolves errors for an embedded property"() {
		given:
		person.address.country = "Australia"

		and:
		def propertyAccessor = factory.accessorFor(person, "address.country")

		expect:
		!person.validate()

		and:
		propertyAccessor.errors.first().code == "inList"
		propertyAccessor.invalid
	}

	@Issue("http://jira.grails.org/browse/GRAILS-7713")
	def "resolves errors for an indexed property"() {
		given:
		author.books[0].title = ""

		and:
		def propertyAccessor = factory.accessorFor(author, "books[0].title")

		expect:
		!author.validate()

		and:
		propertyAccessor.errors.first().code == "blank"
		propertyAccessor.invalid
	}

	@Unroll({ "the $path property is ${expected ? '' : 'not '}required" })
	def "correctly identifies required properties"() {
		given:
		def propertyAccessor = factory.accessorFor(bean, path)

		expect:
		propertyAccessor.required == expected

		where:
		bean   | path          | expected
		person | "name"        | true // non-blank string
		person | "dateOfBirth" | false // nullable object
		person | "password"    | false // blank string
		person | "gender"      | true // non-nullable string
		person | "minor"       | false // boolean properties are never considered required
	}

}

// classes for testing embedded and simple collection properties
@Entity
class Person {
	String name
	String password
	Gender gender
	Date dateOfBirth
	Map emails = [:]
	boolean minor
	static hasMany = [emails: String]
	Address address
	static embedded = ['address']
	static constraints = {
		name blank: false
		dateOfBirth nullable: true
	}
}

@Entity
class Address {
	String street
	String city
	String country
	static constraints = {
		street blank: false
		city blank: false
		country inList: ["USA", "UK", "Canada"]
	}
}

// classes for testing indexed associations
@Entity
class Author {
	String name
	List books
	static hasMany = [books: Book]
	static constraints = {
		name blank: false
	}
}

@Entity
class Book {
	String title
	static belongsTo = [author: Author]
	static constraints = {
		title blank: false
	}
}

enum Gender {
	Male, Female
}