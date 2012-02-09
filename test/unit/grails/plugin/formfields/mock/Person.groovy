package grails.plugin.formfields.mock

import grails.persistence.Entity

@Entity
class Person {
    Salutation salutation
	String name
	String password
	Gender gender
	Date dateOfBirth
	Address address
	Map emails = [:]
	boolean minor
	Date lastUpdated
	String excludedProperty
	String displayFalseProperty

	static hasMany = [emails: String]
	static embedded = ['address']

	static constraints = {
        salutation nullable: true
		name blank: false
		dateOfBirth nullable: true
		address nullable: true
		excludedProperty nullable: true
		displayFalseProperty nullable: true, display: false
	}

	static scaffold = [exclude: ['excludedProperty']]

	def onLoad = {
		println "loaded"
	}

	@Override
	String toString() {
		name
	}
}
