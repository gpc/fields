package grails.plugin.formfields.test

import geb.spock.GebSpec

class CommandBeanSpec extends GebSpec {

	void 'a form using a command object renders correctly'() {
		given:
		go 'command/index'

		expect:
		def form = $('form')
		form.username().@type == 'text'
		form.password().@type == 'password'
	}

}
