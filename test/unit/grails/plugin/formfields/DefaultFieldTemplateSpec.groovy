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
		model.widget = '<input name="property">'
        views["/default/_field.gsp"] = '<g:set var="classes" value="fieldcontain "/>\n' +
                '<g:if test="${required}">\n' +
                '    <g:set var="classes" value="${classes + \'required \'}"/>\n' +
                '</g:if>\n' +
                '<g:if test="${invalid}">\n' +
                '    <g:set var="classes" value="${classes + \'error \'}"/>\n' +
                '</g:if>\n' +
                '<div class="${classes}">\n' +
                '    <label for="${prefix}${property}">${label}<g:if test="${required}"><span class="required-indicator">*</span></g:if></label>\n' +
                '    <%= widget %>\n' +
                '</div>'
	}
	
	static Jerry $(String html) {
		jerry(html).children()
	}
	
	void "default rendering"() {
		when:
		def output = tagLib.renderDefaultField(model)

		then:
		def root = $(output.toString())
		root.is('div.fieldcontain')

		and:
		def label = root.find('label')
		label.text() == 'label'
		label.attr('for') == 'property'
		
		and:
		label.next().is('input[name=property]')
	}

	void "container marked as invalid"() {
		given:
		model.invalid = true

		when:
		def output = tagLib.renderDefaultField(model)
		
		then:
		$(output.toString()).hasClass('error')
	}

	void "container marked as required"() {
		given:
		model.required = true

		when:
		def output = tagLib.renderDefaultField(model)

		then:
		def root = $(output.toString())
		root.hasClass('required')
		
		and:
		def indicator = root.find('label .required-indicator')
		indicator.size()
		indicator.text() == '*'
	}

}
