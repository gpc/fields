package grails.plugin.formfields.mock

import grails.persistence.Entity

@Entity
class Book {
	String title
	static belongsTo = [author: Author]
	static constraints = {
		title blank: false
	}
}
