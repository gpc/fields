package grails.plugin.formfields.mock

class Address {
	String street
	String city
	String country

	@Override
	String toString() { "$street, $city - $country" }

	static constraints = {
		street blank: false
		city blank: false
		country inList: ["USA", "UK", "Canada"]
	}
}
