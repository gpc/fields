package grails.plugin.formfields

import grails.test.mixin.TestFor
import jodd.lagarto.dom.jerry.Jerry
import spock.lang.Specification
import static jodd.lagarto.dom.jerry.Jerry.jerry

@TestFor(FormFieldsTagLib)
class DefaultFieldTemplateSpec extends Specification {
	
	Map model = [:]

	void setup() {
		model.invalid = false
		model.label = 'label'
		model.property = 'property'
		model.required = false
		model.widget = '<input name="property"/>'
	}
	
	static Jerry $(String html) {
		jerry(html).children()
	}
	
	void "default rendering"() {
		when:
		def output = tagLib.renderDefaultField(model)

		then:
		def root = $(output)
		root.is('div.fieldcontain')

		and:
		def label = root.find('label')
		label.text() == 'label'
		label.attr('for') == 'property'
		
		and:
		label.next().is('input[name=property]')
	}

    void "default rendering of showField"() {
        when:
        def output = tagLib.renderDefaultField(model, 'showField')

        then:
        def root = $(output)
        root.is('li.fieldcontain')

        and:
        def label = root.find('.property-label')
        label.text() == 'label'
        label.attr('class') == 'property-label'

        and:
        def value = root.find('.property-value')
        value.html() == model.widget
    }

    void "container marked as invalid"() {
		given:
		model.invalid = true

		when:
		def output = tagLib.renderDefaultField(model)
		
		then:
		$(output).hasClass('error')
	}

	void "container marked as required"() {
		given:
		model.required = true

		when:
		def output = tagLib.renderDefaultField(model)

		then:
		def root = $(output)
		root.hasClass('required')
		
		and:
		def indicator = root.find('label .required-indicator')
		indicator.size()
		indicator.text() == '*'
	}

}
