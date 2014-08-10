package grails.plugin.formfields.test

import geb.spock.GebSpec
import test.Product

class CustomDisplaySpec extends GebSpec {

    void setup() {
        new Product(name: '<b>MacBook</b> Pro', price: 1499.99, taxRate: 0.2).save(failOnError: true)
        new Product(name: 'MacBook Air', price: 1099.99, taxRate: 0.2).save(failOnError: true)
    }

    void 'display is customized on list page'() {
        given:
        go 'product/index'
        def row = $('tbody tr', 0)

        expect:
        row.find('td', 0).find('a').text() == '<b>MacBook</b> Pro'
        row.find('td', 1).text() == '\u00a31,499.99'
        row.find('td', 2).text() == '0.2%'
        row.find('td', 3).text() == '\u00a3300.00'
    }

    void 'display is customized on show page'() {
        given:
        go 'product/index'
        $('tbody tr', 0).find('td a').click()

        expect:
        $('ol li', 0).find('.property-label').text() == 'Name'
        $('ol li', 0).find('.property-value').text() == '<b>MacBook</b> Pro'
        $('ol li', 1).find('.property-label').text() == 'Price'
        $('ol li', 1).find('.property-value').text() == '\u00a31,499.99'
        $('ol li', 2).find('.property-label').text() == 'Tax Rate'
        $('ol li', 2).find('.property-value').text() == '0.2%'
        $('ol li', 3).find('.property-label').text() == 'Tax'
        $('ol li', 3).find('.property-value').text() == '\u00a3300.00'
    }

}
