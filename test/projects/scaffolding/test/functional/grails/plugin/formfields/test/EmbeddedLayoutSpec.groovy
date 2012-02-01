package grails.plugin.formfields.test

import geb.spock.GebSpec

class EmbeddedLayoutSpec extends GebSpec {

	void 'embedded field sitemesh layout can be overridden in an application'() {
		given:
		go 'person/create'

		expect:
		$('fieldset.address').hasClass('custom')
	}
}
