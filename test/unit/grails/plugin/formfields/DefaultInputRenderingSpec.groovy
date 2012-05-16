package grails.plugin.formfields

import grails.plugin.formfields.mock.Person
import grails.util.Environment
import grails.test.mixin.*
import org.codehaus.groovy.grails.commons.*
import spock.lang.*

@TestFor(FormFieldsTagLib)
@Mock(Person)
@Unroll
class DefaultInputRenderingSpec extends Specification {

	@Shared def personDomainClass = new DefaultGrailsDomainClass(Person)
	@Shared def basicProperty = new MockPersistentProperty()
	@Shared def oneToOneProperty = new MockPersistentProperty(oneToOne: true, referencedPropertyType: Person, referencedDomainClass: personDomainClass)
	@Shared def manyToOneProperty = new MockPersistentProperty(manyToOne: true, referencedPropertyType: Person, referencedDomainClass: personDomainClass)
	@Shared def manyToManyProperty = new MockPersistentProperty(manyToMany: true, referencedPropertyType: Person, referencedDomainClass: personDomainClass)
	@Shared def oneToManyProperty = new MockPersistentProperty(oneToMany: true, referencedPropertyType: Person, referencedDomainClass: personDomainClass)
	@Shared List<Person> people

	void setupSpec() {
		people = ["Bart Simpson", "Homer Simpson", "Monty Burns"].collect {
			new Person(name: it)
		}
	}
	
	void setup() {
		people*.save(validate: false)
	}

    private List<Person> getSimpsons() { people.findAll { it.name.contains("Simpson")} }

	def "input for a #type.simpleName property matches '#outputPattern'"() {
		given:
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: basicProperty]

		expect:
		tagLib.renderDefaultInput(model) =~ outputPattern

		where:
		type    | outputPattern
		String  | /input type="text"/
		Boolean | /input type="checkbox"/
		boolean | /input type="checkbox"/
		int     | /input type="number"/
		Integer | /input type="number"/
		URL     | /input type="url"/
		Byte[]  | /input type="file"/
		byte[]  | /input type="file"/
	}

	def "input for a #type.simpleName property with a value of '#value' matches '#outputPattern'"() {
		given:
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: basicProperty, value: value]

		expect:
		tagLib.renderDefaultInput(model) =~ outputPattern

		where:
		type    | value                | outputPattern
		String  | "catflap"            | /value="catflap"/
		Boolean | Boolean.TRUE         | /checked="checked"/
		boolean | true                 | /checked="checked"/
		int     | 1337                 | /value="1337"/
		Integer | new Integer(1337)    | /value="1337"/
		URL     | "http://grails.org/" | /value="http:\/\/grails.org\/"/
	}

	@Issue('https://github.com/robfletcher/grails-fields/issues/60')
	void "input for a property with a password constraint does not include the value"() {
		given:
		def model = [type: String, property: "prop", constraints: [password: true], persistentProperty: basicProperty, value: 'correct horse battery staple']

		expect:
		tagLib.renderDefaultInput(model).contains('value=""')
	}

	def "input for a #{required ? 'a required' : 'an optional'} property #{required ? 'has' : 'does not have'} the required attribute"() {
		given:
		def model = [type: String, property: "prop", required: required, constraints: [:], persistentProperty: basicProperty]

		expect:
		tagLib.renderDefaultInput(model).contains('required=""') ^ !required

		where:
		required << [true, false]
	}

	def "input for #{invalid ? 'an invalid' : 'a valid'} property #{invalid ? 'has' : 'does not have'} the invalid attribute"() {
		given:
		def model = [type: String, property: "prop", invalid: invalid, constraints: [:], persistentProperty: basicProperty]

		expect:
		tagLib.renderDefaultInput(model).contains('invalid=""') ^ !invalid

		where:
		invalid << [true, false]
	}

	def "input for an enum property is a select"() {
		given:
		def model = [type: Environment, property: "prop", constraints: [:], persistentProperty: basicProperty]

		when:
		def output = tagLib.renderDefaultInput(model)

		then:
		output =~ /select name="prop"/
		Environment.values().every {
			output =~ /option value="$it"/
		}
	}

	def "enum select has correct selected option"() {
		given:
		def model = [type: Environment, property: "prop", constraints: [:], persistentProperty: basicProperty, value: Environment.PRODUCTION]

		when:
		def output = tagLib.renderDefaultInput(model)

		then:
		output =~ /<option value="PRODUCTION" selected="selected"/
	}

	def "input for a #type.simpleName property is a special select type"() {
		given:
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: basicProperty]

		expect:
		tagLib.renderDefaultInput(model) =~ outputPattern

		where:
		type          | outputPattern
		Date          | /select name="prop_day"/
		Calendar      | /select name="prop_day"/
		java.sql.Date | /select name="prop_day"/
		java.sql.Time | /select name="prop_day"/
		TimeZone      | /<option value="Europe\/London"/
		Locale        | /<option value="en_GB"/
		Currency      | /<option value="GBP"/
	}

	def "input for a #type.simpleName property has the correct option(s) selected"() {
		given:
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: basicProperty, value: value]

		expect:
		tagLib.renderDefaultInput(model) =~ outputPattern

		where:
		type          | value                                 | outputPattern
		Date          | new Date(108, 9, 2)                   | /option value="2008" selected="selected"/
		Calendar      | new GregorianCalendar(2008, 9, 2)     | /option value="2008" selected="selected"/
		java.sql.Date | new java.sql.Date(108, 9, 2)          | /option value="2008" selected="selected"/
		java.sql.Time | new java.sql.Time(13, 29, 1)          | /option value="13" selected="selected"/
		TimeZone      | TimeZone.getTimeZone("Europe/London") | /<option value="Europe\/London" selected="selected"/
		Locale        | Locale.ITALIAN                        | /<option value="it" selected="selected"/
		Currency      | Currency.getInstance("USD")           | /<option value="USD" selected="selected"/
	}

	def "select for #{required ? 'a required' : 'an optional'} #type.simpleName property #{required ? 'does not have' : 'has'} a no-selection option"() {
		given:
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: basicProperty, required: required]

		when:
		def output = tagLib.renderDefaultInput(model)

		then:
		output.contains('<option value=""') ^ required

		where:
		type          | required
		Date          | true
		Calendar      | true
		java.sql.Date | true
		java.sql.Time | true
		Date          | false
		Calendar      | false
		java.sql.Date | false
		java.sql.Time | false
	}

	def "select for a #type.simpleName property has a precision of 'day'"() {
		given:
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: basicProperty]

		when:
		def output = tagLib.renderDefaultInput(model)

		then:
		output.contains('select name="prop_year"')
		output.contains('select name="prop_month"')
		output.contains('select name="prop_day"')
		!output.contains('select name="prop_hour"')
		!output.contains('select name="prop_minute"')

		where:
		type << [Date, Calendar, java.sql.Date]
	}

	def "select for a Time property has a precision of 'minute'"() {
		given:
		def model = [type: java.sql.Time, property: "prop", constraints: [:], persistentProperty: basicProperty]

		when:
		def output = tagLib.renderDefaultInput(model)

		then:
		output.contains('select name="prop_year"')
		output.contains('select name="prop_month"')
		output.contains('select name="prop_day"')
		output.contains('select name="prop_hour"')
		output.contains('select name="prop_minute"')
	}

	def "select for #{required ? 'a required' : 'an optional'} #type.simpleName property #{required ? 'does not have' : 'has'} a no-selection option"() {
		given:
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: basicProperty, required: required]

		expect:
		tagLib.renderDefaultInput(model).contains('<option value=""></option>') ^ required

		where:
		type     | required
		TimeZone | true
		Locale   | true
		Currency | true
		TimeZone | false
		Locale   | false
		Currency | false
	}

	def "input for a String property with #constraints constraints matches #outputPattern"() {
		given:
		def model = [type: String, property: "prop", constraints: constraints, persistentProperty: basicProperty]

		expect:
		tagLib.renderDefaultInput(model) =~ outputPattern

		where:
		constraints      | outputPattern
		[email: true]    | /type="email"/
		[url: true]      | /type="url"/
		[password: true] | /type="password"/
	}

	def "input for a numeric property with a range constraint is a range"() {
		given:
		def model = [type: Integer, property: "prop", constraints: [range: (0..10)], persistentProperty: basicProperty]

		when:
		def output = tagLib.renderDefaultInput(model)

		then:
		output =~ /input type="range"/
		output =~ /min="0"/
		output =~ /max="10"/
	}

	def "input for a #type.simpleName property with #constraints constraints matches #outputPattern"() {
		given:
		def model = [type: type, property: "prop", constraints: constraints, persistentProperty: basicProperty]

		expect:
		tagLib.renderDefaultInput(model) =~ outputPattern

		where:
		type   | constraints       | outputPattern
		int    | [min: 0]          | /min="0"/
		int    | [max: 10]         | /max="10"/
		int    | [min: 0, max: 10] | /min="0"/
		int    | [min: 0, max: 10] | /max="10"/
		String | [maxSize: 32]     | /maxlength="32"/
		String | [matches: /\d+/]  | /pattern="\\d\+"/
		String | [editable: false] | /readonly=""/
	}

	def "input for a #type.simpleName property with an inList constraint of #inListConstraint is a select"() {
		given:
		def model = [type: type, property: "prop", constraints: [inList: inListConstraint], persistentProperty: basicProperty]

		when:
		def output = tagLib.renderDefaultInput(model)

		then:
		output =~ /select name="prop"/
		inListConstraint.every {
			output =~ /option value="$it"/
		}

		where:
		type   | inListConstraint
		int    | [1, 3, 5]
		String | ["catflap", "rubberplant", "marzipan"]
	}

	def "input for an optional #type.simpleName property #{constraints ? 'with #constraints constraints ' : ''}has a no-selection option"() {
		given:
		def model = [type: type, property: "prop", constraints: constraints, persistentProperty: basicProperty]

		expect:
		tagLib.renderDefaultInput(model) =~ /<option value=""><\/option>/

		where:
		type        | constraints
		Environment | [:]
		int         | [inList: [1, 3, 5]]
		String      | [inList: ["catflap", "rubberplant", "marzipan"]]
	}

	def "input for a required #type.simpleName property #{constraints ? 'with #constraints constraints ' : ''}has a no-selection option"() {
		given:
		def model = [type: type, property: "prop", constraints: constraints, required: true, persistentProperty: basicProperty]

		expect:
		!(tagLib.renderDefaultInput(model) =~ /<option value=""><\/option>/)

		where:
		type        | constraints
		Environment | [:]
		int         | [inList: [1, 3, 5]]
		String      | [inList: ["catflap", "rubberplant", "marzipan"]]
	}

	def "input for a #description property is a select"() {
		given:
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: persistentProperty]

		when:
		def output = tagLib.renderDefaultInput(model)

		then:
		output =~ /select name="prop/
		output =~ /id="prop"/
		people.every {
			output =~ /option value="$it.id" >$it.name/
		}

		where:
		type   | persistentProperty | description
		Person | oneToOneProperty   | "one-to-one"
		Person | manyToOneProperty  | "many-to-one"
		Set    | manyToManyProperty | "many-to-many"
	}

	def "input for a #description property doesn't have `.id` at the end of the name"() {
		given:
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: persistentProperty]

		when:
		def output = tagLib.renderDefaultInput(model)

		then:
		output =~ /select name="prop"/
		output =~ /id="prop"/
		people.every {
			output =~ /option value="$it.id" >$it.name/
		}

		where:
		type   | persistentProperty | description
		Set    | manyToManyProperty | "many-to-many"
	}

	def "input for a #description property does have `.id` at the end of the name"() {
		given:
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: persistentProperty]

		when:
		def output = tagLib.renderDefaultInput(model)

		then:
		output =~ /select name="prop.id"/
		output =~ /id="prop"/
		people.every {
			output =~ /option value="$it.id" >$it.name/
		}

		where:
		type   | persistentProperty | description
		Person | oneToOneProperty   | "one-to-one"
		Person | manyToOneProperty  | "many-to-one"
	}

    def "input for a #description property is a select containing only entries specified in from parameter"() {
        given:
        def model = [type: type, property: "prop", constraints: [:], persistentProperty: persistentProperty]

        when:
        def output = tagLib.renderDefaultInput(model, [from: simpsons])

        then:
        output =~ /select name="prop/
        output =~ /id="prop"/
        simpsons.every {
            output =~ /option value="$it.id" >$it.name/
        }
        !output.contains("Monty Burns")

        where:
        type   | persistentProperty | description
        Person | oneToOneProperty   | "one-to-one"
        Person | manyToOneProperty  | "many-to-one"
        Set    | manyToManyProperty | "many-to-many"
    }

	def "select for a #description property with a value of #value has the correct option selected"() {
		given:
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: persistentProperty, value: value]

		when:
		def output = tagLib.renderDefaultInput(model)

		then:
		output =~ /option value="${people[1].id}" selected="selected" >${people[1].name}/

		where:
		type   | persistentProperty | description    | value
		Person | oneToOneProperty   | "one-to-one"   | people[1]
		Person | manyToOneProperty  | "many-to-one"  | people[1]
		Set    | manyToManyProperty | "many-to-many" | [people[1]]
	}

	def "input for #{required ? 'a required' : 'an optional'} #description property #{required ? 'has' : 'does not have'} a no-selection option"() {
		given:
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: persistentProperty, required: required]

		expect:
		tagLib.renderDefaultInput(model).contains('<option value="null"></option>') ^ required

		where:
		type   | persistentProperty | required | description
		Person | oneToOneProperty   | true     | "one-to-one"
		Person | manyToOneProperty  | true     | "many-to-one"
		Person | oneToOneProperty   | false    | "one-to-one"
		Person | manyToOneProperty  | false    | "many-to-one"
	}

	def "select for a many-to-many property has the multiple attribute"() {
		given:
		def model = [type: Set, property: "prop", constraints: [:], persistentProperty: manyToManyProperty]

		expect:
		tagLib.renderDefaultInput(model) =~ /multiple=""/
	}

	def "a one-to-many property has a list of links instead of an input"() {
		given:
		messageSource.addMessage("default.add.label", request.locale, "Add {0}")

		and:
		def model = [bean: [id: 1337], beanClass: [propertyName: "thing"], type: Set, property: "prop", constraints: [:], persistentProperty: oneToManyProperty, value: people]

		when:
		def output = tagLib.renderDefaultInput(model)

		then:
		people.every {
			output =~ /<a href="\/person\/show\/$it.id">$it.name<\/a>/
		}

		and:
		output.contains("""<a href="/person/create?thing.id=1337">Add Person</a>""")
	}
	
	@Issue('https://github.com/robfletcher/grails-fields/issues/56')
	def 'an enum with a toString method uses name instead of toString for the keys'() {
		given:
		def model = [type: EnumWithToString, property: "prop", constraints: [:], persistentProperty: basicProperty]

		when:
		def output = tagLib.renderDefaultInput(model)

		then:
		output =~ /select name="prop"/
		EnumWithToString.values().every {
			output =~ /option value="${it.name()}" >${it.toString()}</
		}
	}

	@Issue('https://github.com/robfletcher/grails-fields/issues/56')
	def "enum with toString select and #type value has correct selected option"() {
		when:
		def model = [type: EnumWithToString, property: "prop", constraints: [:], persistentProperty: basicProperty, value: value]
		def output = tagLib.renderDefaultInput(model)

		then:
		output =~ /<option value="FIRST" selected="selected"/
		
		where:
		type 	| value
		"String"| EnumWithToString.FIRST
		"Enum"	| "FIRST"
	}

	@Issue("https://github.com/robfletcher/grails-fields/issues/50")
	def "string property with a widget type of textarea is rendered as a textArea"() {
		given:
		def model = [type: String, property: "prop", constraints: [widget:'textarea'], persistentProperty: basicProperty]

		expect:
		tagLib.renderDefaultInput(model) =~ /textarea name="prop"/
	}

	@Issue('https://github.com/robfletcher/grails-fields/issues/43')
	void 'input type can be overridden by supplying input-type parameter'() {
		given:
		def model = [type: propertyType, property: "prop", constraints: [:], persistentProperty: basicProperty]

		expect:
		tagLib.renderDefaultInput(model, [type: typeAttribute]) =~ outputPattern

		where:
		propertyType | typeAttribute | outputPattern
		String       | 'search'      | /input type="search"/
		int          | 'range'       | /input type="range"/
		Integer      | 'range'       | /input type="range"/
	}

}

class MockPersistentProperty implements GrailsDomainClassProperty {
	boolean association
	boolean basicCollectionType
	boolean bidirectional
	boolean circular
	GrailsDomainClass component
	boolean derived
	GrailsDomainClass domainClass
	boolean embedded
	int fetchMode
	String fieldName
	boolean hasOne
	boolean identity
	boolean inherited
	boolean manyToMany
	boolean manyToOne
	String name
	boolean oneToMany
	boolean oneToOne
	boolean optional
	GrailsDomainClassProperty otherSide
	boolean owningSide
	String naturalName
	boolean persistent
	GrailsDomainClass referencedDomainClass
	String referencedPropertyName
	Class referencedPropertyType
	Class type
	String typePropertyName
	boolean isEnum

	boolean isEnum() { isEnum }
}

enum EnumWithToString {
	FIRST("first"),
	SECOND("second"),
	THIRD("third")
	
	private String str
	
	private EnumWithToString(String str) {
		this.str = str
	}
	
	public String toString() {
		return str
	}
}
