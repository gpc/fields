package test

class Product {

	String name
	Double price
	Double taxRate
	Double tax

	static constraints = {
		name blank: false
		price min: 0.0d, scale: 2
		taxRate min: 0.0d, scale: 2
	}

	static mapping = {
		tax formula: 'price * tax_rate'
	}
}
