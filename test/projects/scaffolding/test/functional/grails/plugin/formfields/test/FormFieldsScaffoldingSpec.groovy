package grails.plugin.formfields.test

import spock.lang.Stepwise

@Stepwise
class FormFieldsScaffoldingSpec extends GebSpec {

	void setupSpec() {
		go 'person/create'
	}

	void 'create form includes all fields'() {
		given:
		def inputNames = $('input')*.@name

		expect: 'input fields are present'
		'name' in inputNames
		'address.street' in inputNames
		'address.city' in inputNames
		'address.country' in inputNames
	}

	void 'can save a new instance'() {
		given:
		def form = $('form')

		when:
		form.name = 'Bart Simpson'
		form.'address.street' = '54 Evergreen Terrace'
		form.'address.city' = 'Springfield'
		form.'address.country' = 'USA'
		form.create().click()

		then:
		$('.message').text() ==~ /Person \d+ created/
	}

	void 'values are populated on edit form'() {
		when:
		$('a.edit').click()

		then:
		title == 'Edit Person'

		and:
		def form = $('form')
		form.name == 'Bart Simpson'
		form.'address.street' == '54 Evergreen Terrace'
		form.'address.city' == 'Springfield'
		form.'address.country' == 'USA'
	}

}
