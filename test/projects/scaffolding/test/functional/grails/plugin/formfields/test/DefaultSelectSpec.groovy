package grails.plugin.formfields.test

import geb.spock.GebSpec
import spock.lang.Stepwise

@Stepwise
class DefaultSelectSpec extends GebSpec {

    void 'create some captains'() {
        given:
        go 'captain/create'
        def form = $('form')

        when:
        form.name = name
        form.create().click()

        then:
        $('.message').text() ==~ /Captain \d+ created/

        where:
        name << ["Kirk", "Khan"]
    }

    void 'captains are listed by name when creating ship'() {
        given:
        go 'ship/create'

        when:
        def options = $('select option')

        then:
        options*.text().sort() == ["Khan", "Kirk"]
    }

    void 'kirk is captain of the enterprise'() {
        given:
        def form = $('form')

        when:
        form.'captain.id' = "Kirk"
        form.name = "Enterprise"

        form.create().click()

        then:
        $('.message').text() ==~ /Ship \d+ created/
    }

}
