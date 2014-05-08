package grails.plugin.fields

import grails.plugin.formfields.BeanPropertyAccessorFactory
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.hibernate.proxy.HibernateProxy
import spock.lang.*
import test.*

@Issue('https://github.com/robfletcher/grails-fields/issues/36')
class HibernateProxyHandlingSpec extends Specification {

	BeanPropertyAccessorFactory beanPropertyAccessorFactory

	void setup() {
		beanPropertyAccessorFactory = ApplicationHolder.application.mainContext.beanPropertyAccessorFactory

		Cocktail.withNewSession {
			def cocktail = new Cocktail(name: 'Old Fashioned')
			cocktail.addToIngredients new Ingredient(name: 'Bourbon')
			cocktail.addToIngredients new Ingredient(name: 'Angostura bitters')
			cocktail.addToIngredients new Ingredient(name: 'Brown sugar')
			cocktail.save(failOnError: true, flush: true)
		}
	}

	void 'hibernate proxy gets resolved'() {
		given:
		def cocktail = Cocktail.findByName('Old Fashioned')

		and:
		def propertyAccessor = beanPropertyAccessorFactory.accessorFor(cocktail, 'ingredients[0].id')

		expect:
		cocktail.ingredients[0] instanceof HibernateProxy

		and:
		propertyAccessor.beanType == Ingredient
		propertyAccessor.labelKeys == ['cocktail.ingredients.id.label', 'ingredient.id.label']
	}

}
