package test

class Product {

	String name
	Double price
	Double taxRate
	Double tax

	static constraints = {
		name blank: false
		price min: 0.0d
		taxRate min: 0.0d
	}

	static mapping = {
		tax formula: 'price * tax_rate'
	}
}
