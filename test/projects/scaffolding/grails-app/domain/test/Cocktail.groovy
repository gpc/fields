package test

class Cocktail {
	
	String name
	List<Ingredient> ingredients

	static hasMany = [ingredients: Ingredient]

	static constraints = {
		name blank: false, unique: true
		ingredients minSize: 3 // http://www.imdb.com/title/tt1615667/quotes?qt=qt1305438
	}
}
