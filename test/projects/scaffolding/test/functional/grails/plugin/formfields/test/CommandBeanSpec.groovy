package grails.plugin.formfields.test

class CommandBeanSpec extends GebSpec {

	void 'a form using a command object renders correctly'() {
		given:
		go 'command/index'

		expect:
		$('form').with {
			username().@type == 'text'
			password().@type == 'password'
		}
	}

}
