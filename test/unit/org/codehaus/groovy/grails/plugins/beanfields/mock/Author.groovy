package org.codehaus.groovy.grails.plugins.beanfields.mock

import grails.persistence.Entity

@Entity
class Author {
	String name
	List books
	static hasMany = [books: Book]
	static constraints = {
		name blank: false
	}
}
