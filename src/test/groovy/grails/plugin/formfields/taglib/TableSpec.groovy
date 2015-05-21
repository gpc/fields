package grails.plugin.formfields.taglib
import grails.plugin.formfields.FormFieldsTagLib
import grails.plugin.formfields.mock.Employee
import grails.plugin.formfields.mock.Person
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Issue
import spock.lang.Unroll

import static grails.plugin.formfields.mock.Gender.Female
import static grails.plugin.formfields.mock.Gender.Male

@TestFor(FormFieldsTagLib)
@Mock(Person)
@Unroll
class TableSpec extends AbstractFormFieldsTagLibSpec {
    def personList = [new Person(name: "Bart Simpson", gender: Male),
                      new Person(name: "Marge Simpson", gender: Female)]

    def setupSpec() {
        configurePropertyAccessorSpringBean()
    }

    def setup() {
        def taglib = applicationContext.getBean(FormFieldsTagLib)
        mockEmbeddedSitemeshLayout(taglib)
    }

    void "body tag renders columns for the first 7 properties ordered by the domain class constraints"() {
        when:
        def output = applyTemplate('<f:table collection="collection"/>', [collection: personList])
        def table = new XmlSlurper().parseText(output)

        then:
        table.thead.tr.th.a.collect {it.text().trim()} == ['Salutation', 'Name', 'Date Of Birth', 'Address', 'Excluded Property', 'Display False Property', 'Grails Developer']
        table.tbody.tr.collect { it.td[1].text() } == ['Bart Simpson', 'Marge Simpson']
    }

    @Issue('https://github.com/grails3-plugins/fields/issues/3')
    void "body tag allows to specify the domain class"() {
        given:
        def mixedPersonList = [new Employee(name: "Homer Simpson", salary: 1)] + personList

        when:
        def output = applyTemplate('<f:table collection="collection" domainClass="grails.plugin.formfields.mock.Person"/>', [collection: mixedPersonList])
        def table = new XmlSlurper().parseText(output)

        then:
        table.thead.tr.th.a.collect {it.text().trim()} == ['Salutation', 'Name', 'Date Of Birth', 'Address', 'Excluded Property', 'Display False Property', 'Grails Developer']
        table.tbody.tr.collect { it.td[1].text() } == ['Homer Simpson', 'Bart Simpson', 'Marge Simpson']
    }

    @Issue('https://github.com/grails3-plugins/fields/issues/3')
    void "body tag allows to specify the properties to be shown"() {
        when:
        def output = applyTemplate('<f:table collection="collection" properties="[\'gender\', \'name\']"/>', [collection: personList])
        println output
        def table = new XmlSlurper().parseText(output)

        then:
        table.thead.tr.th.a.collect {it.text().trim()} == ['Gender', 'Name']
        table.tbody.tr.collect { it.td[0].text() } == ['Male', 'Female']
    }

}