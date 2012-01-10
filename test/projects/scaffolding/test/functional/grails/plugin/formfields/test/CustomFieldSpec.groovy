package grails.plugin.formfields.test

import geb.spock.GebSpec

class CustomFieldSpec extends GebSpec {

	void 'a form using custom fields renders correctly'() {
		given:
		go 'custom/index'

		expect: 'the form elements should have the correct type'
		def form = $('form')
		form.username().@type == 'text'
		form.password().@type == 'password'

		and: 'values should be empty'
		form.username().@value == ''
		form.password().@value == ''

		and: 'neither field should be flagged as required'
		!form.username().parent().hasClass('required')
		!form.password().parent().hasClass('required')
	}

}
