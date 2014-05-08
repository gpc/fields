package grails.plugin.formfields.test

import geb.spock.GebSpec
import spock.lang.Shared

class CustomFieldSpec extends GebSpec {

    @Shared def form

    void setupSpec() {
        go 'custom/index'
        form = $('form')
    }

	void 'inputs are text by default'() {
		expect:
        form.username().@type == 'text'
    }

    void 'tag bodies can override the input'() {
        form.password().@type == 'password'
    }

    void 'values should be empty as there is no bean'() {
		expect:
        form.username().@value == ''
        form.password().@value == ''
    }

    void 'fields should not be required as they have no constraints'() {
		expect:
		!form.username().parent().hasClass('required')
		!form.password().parent().hasClass('required')
    }

    void 'labels should be defaulted based on the property name'() {
        expect:
        form.username().previous('label').text() == 'Username'
        form.username().previous('label').@for == 'username'
        form.password().previous('label').text() == 'Password'
        form.password().previous('label').@for == 'password'
	}

}
