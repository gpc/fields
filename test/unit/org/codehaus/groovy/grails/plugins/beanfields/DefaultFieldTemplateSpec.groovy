package org.codehaus.groovy.grails.plugins.beanfields

import grails.test.mixin.TestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin
import jodd.lagarto.dom.jerry.Jerry
import spock.lang.Specification
import static jodd.lagarto.dom.jerry.Jerry.jerry

@TestMixin(GroovyPageUnitTestMixin)
class DefaultFieldTemplateSpec extends Specification {
	
	Map model = [:]

	void setup() {
		model.invalid = false
		model.label = 'label'
		model.property = 'property'
		model.required = false
		model.widget = '<input name="property">'
	}
	
	static Jerry $(String html) {
		jerry(html).children()
	}
	
	void "default rendering"() {
		when:
		def output = render(template: '/forms/default/field', model: model)
		
		then:
		def root = $(output)
		root.get(0).nodeName == 'div'
		root.hasClass('fieldcontain')
		
		and:
		def label = root.find('label')
		label.text() == 'label'
		label.attr('for') == 'property'
		
		and:
		def input = label.next()
		input.get(0).nodeName == 'input'
		input.attr('name') == 'property'
	}

	void "container marked as invalid"() {
		given:
		model.invalid = true

		when:
		def output = render(template: '/forms/default/field', model: model)
		
		then:
		$(output).hasClass('error')
	}

	void "container marked as required"() {
		given:
		model.required = true

		when:
		def output = render(template: '/forms/default/field', model: model)
		
		then:
		def root = $(output)
		root.hasClass('required')
		
		and:
		def indicator = root.find('label .required-indicator')
		indicator.size()
		indicator.text() == '*'
	}

}
