package grails.plugin.formfields.test

import geb.spock.GebSpec
import spock.lang.Issue

@Issue('https://github.com/robfletcher/grails-fields/issues/85')
class DerivedPropertySpec extends GebSpec {

	void 'derived property fields are not displayed'() {
		given:
		go 'product/create'

		expect:
		!$('input[name=tax]')
	}

}
