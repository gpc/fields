package grails.plugin.formfields

import grails.plugin.formfields.mock.Person
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.util.Environment
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import org.grails.datastore.mapping.model.types.OneToMany
import groovy.mock.interceptor.MockFor
import org.grails.datastore.mapping.model.types.ManyToOne
import org.grails.datastore.mapping.model.types.ManyToMany
import org.grails.datastore.mapping.model.PropertyMapping
import java.beans.PropertyDescriptor
import org.grails.datastore.mapping.model.types.OneToOne

@TestFor(FormFieldsTagLib)
@Mock(Person)
@Unroll
class DefaultShowRenderingSpec extends Specification {

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

	def "simple property renders correctly"() {
        def bart = Person.findByName("Bart Simpson")
        given:
		def model = [bean: bart, property: "name", persistentProperty: basicProperty]

		expect:
		bart.name == tagLib.renderDefaultShow(model)
	}

    def "input for a #description property is a select containing only entries specified in from parameter"() {
        given:
        def model = [type: type, property: "prop", persistentProperty: persistentProperty, value: value]

        when:
        def output = tagLib.renderDefaultShow(model, [from: simpsons])

        then:
        output == expected

        where:
        type   | persistentProperty | value              | expected
        Person | oneToOneProperty   | [id: 1]            | '<a href="/person/show/1">[id:1]</a>'
        Person | manyToOneProperty  | [id: 1]            | '<a href="/person/show/1">[id:1]</a>'
        Set    | manyToManyProperty | [[id: 1], [id: 2]] | '<ul><li><a href="/person/show/1">[id:1]</a></li><li><a href="/person/show/2">[id:2]</a></li></ul>'
        Set    | oneToManyProperty  | [[id: 1], [id: 2]] | '<ul><li><a href="/person/show/1">[id:1]</a></li><li><a href="/person/show/2">[id:2]</a></li></ul>'
    }

}


