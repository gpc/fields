package grails.plugin.formfields.taglib
import grails.plugin.formfields.FormFieldsTagLib
import grails.plugin.formfields.FormFieldsTemplateService
import grails.plugin.formfields.mock.Address
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
    def address = new Address(street: '742 Evergreen Terrace', city: 'Springfield', country: 'USA')
    def bart = new Person(name: "Bart Simpson", gender: Male, address: address)
    def marge = new Person(name: "Marge Simpson", gender: Female, address: address)
    def personList = [bart, marge]

    def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

    def setupSpec() {
        configurePropertyAccessorSpringBean()
    }

    def setup() {
        def taglib = applicationContext.getBean(FormFieldsTagLib)
        taglib.formFieldsTemplateService = mockFormFieldsTemplateService
        mockEmbeddedSitemeshLayout(taglib)
    }

    void "table tag renders columns for the first 7 properties ordered by the domain class constraints"() {
        when:
        def output = applyTemplate('<f:table collection="collection"/>', [collection: personList])
        def table = new XmlSlurper().parseText(output)

        then:
        table.thead.tr.th.a.collect {it.text().trim()} == ['Salutation', 'Name', 'Date Of Birth', 'Address', 'Excluded Property', 'Display False Property', 'Grails Developer']
        table.tbody.tr.collect { it.td[1].text() } == ['Bart Simpson', 'Marge Simpson']
    }

    @Issue('https://github.com/grails3-plugins/fields/issues/3')
    void "table tag allows to specify the domain class"() {
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
    void "table tag allows to specify the properties to be shown"() {
        when:
        def output = applyTemplate('<f:table collection="collection" properties="[\'gender\', \'name\']"/>', [collection: personList])
        def table = new XmlSlurper().parseText(output)

        then:
        table.thead.tr.th.a.collect {it.text().trim()} == ['Gender', 'Name']
        table.tbody.tr.collect { it.td[0].text() } == ['Male', 'Female']
    }

    void "table tag displays embedded properties by default with toString"() {
        when:
        def output = applyTemplate('<f:table collection="collection" properties="[\'address\']"/>', [collection: personList])
        def table = new XmlSlurper().parseText(output)

        then:
        table.thead.tr.th.a.collect {it.text().trim()} == ['Address']
        table.tbody.tr.collect { it.td[0].text() } == [address.toString()] * 2
    }

    void "table tag uses custom display template when displayStyle is specified"() {
        given:
        views["/_fields/display/_custom.gsp"] = 'Custom: ${value}'
        mockFormFieldsTemplateService.findTemplate(_, 'displayWidget-custom', _, null) >> [path: '/_fields/display/custom']

        when:

        def output = applyTemplate('<f:table collection="collection" properties="[\'address\']" displayStyle="custom"/>', [collection: personList])
        def table = new XmlSlurper().parseText(output)

        then:
        table.thead.tr.th.a.collect {it.text().trim()} == ['Address']
        table.tbody.tr.collect { it.td[0].text() } == ["Custom: $address"] * 2
    }

    void "table tag uses custom display template from theme when displayStyle and theme is specified"() {
        given:
        views["/_fields/_themes/test/display/_custom.gsp"] = 'Theme: ${value}'
        mockFormFieldsTemplateService.findTemplate(_, 'displayWidget-custom', _, "test") >> [path: '/_fields/_themes/test/display/custom']

        when:

        def output = applyTemplate('<f:table collection="collection" properties="[\'address\']" displayStyle="custom" theme="test"/>', [collection: personList])
        def table = new XmlSlurper().parseText(output)

        then:
        table.thead.tr.th.a.collect {it.text().trim()} == ['Address']
        table.tbody.tr.collect { it.td[0].text() } == ["Theme: $address"] * 2
    }

    void "table renders full embedded beans when displayStyle= 'default'"() {
        when:

        def output = applyTemplate('<f:table collection="collection" properties="[\'address\']" displayStyle="default"/>', [collection: [bart]])
        def table = new XmlSlurper().parseText(output)
        println output

        then:
        table.thead.tr.th.a.collect {it.text().trim()} == ['Address']
        table.tbody.tr.td.a.ol.li.collect { it.div.text() } == [address.street, address.city, address.country]
    }

}