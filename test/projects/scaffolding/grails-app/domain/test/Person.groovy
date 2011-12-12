package test

class Person {
	
	String name
	Address address

	static embedded = ['address']

    static constraints = {
		name blank: false
    }
}
