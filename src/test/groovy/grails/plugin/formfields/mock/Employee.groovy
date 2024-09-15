package grails.plugin.formfields.mock

import grails.persistence.Entity

@Entity
class Employee { // inheritance currently does not work in Grails 7 // extends Person {
    // extended from Person - start
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
    String biography

    transient String transientText = "transient text"

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
        anotherPicture nullable: true
        password password: true
        biography nullable: true, widget: 'textarea'
    }

    static scaffold = [exclude: ['excludedProperty']]
//    static transients = ['transientText']
    def onLoad = {
        println "loaded"
    }
    // extended from Person - finish

    @Override
    String toString() {
        name
    }

    int salary

    int getYearlySalary() {
        200 * salary
    }

    static transients = ['transientText','yearlySalary']
}
