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
	Boolean grailsDeveloper
    Byte[] picture

	static hasMany = [emails: String]
	static embedded = ['address']

	static constraints = {
        salutation nullable: true
		name blank: false
		dateOfBirth nullable: true
		address nullable: true
		excludedProperty nullable: true
		displayFalseProperty nullable: true, display: false
		grailsDeveloper nullable: true
        picture nullable: true
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
