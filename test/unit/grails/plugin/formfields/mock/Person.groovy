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
	byte[] anotherPicture

    transient String getTransientText() {
        "transient text"
    }

	static hasMany = [emails: String]
	static embedded = ['address']

	static constraints = {
        salutation nullable: true
		name blank: false, widget: 'fancywidget'
		dateOfBirth nullable: true
		address nullable: true
		excludedProperty nullable: true
		displayFalseProperty nullable: true, display: false
		grailsDeveloper nullable: true
		picture nullable: true
		anotherPicture nullable: true
		password password: true
	}

	static scaffold = [exclude: ['excludedProperty']]
    static transients = ['transientText']
	def onLoad = {
		println "loaded"
	}

	@Override
	String toString() {
		name
	}
}
