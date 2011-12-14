package org.codehaus.groovy.grails.plugins.beanfields.templates

import grails.test.mixin.TestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin
import spock.lang.Specification

@TestMixin(GroovyPageUnitTestMixin)
class DefaultFieldTemplateSpec extends Specification {
	
	Map model = [:]

	void setup() {
		model.invalid = false
		model.label = 'label'
		model.property = 'property'
		model.required = false
		model.widget = 'widget'
	}
	
	void "default rendering"() {
		expect:
		render(template: '/forms/default/field', model: model) == '''<div class="fieldcontain  ">
	<label for="property">label</label>
	widget
</div>'''
	}

	void "container marked as invalid"() {
		given:
		model.invalid = true

		expect:
		render(template: '/forms/default/field', model: model) == '''<div class="fieldcontain error ">
	<label for="property">label</label>
	widget
</div>'''
	}

	void "container marked as required"() {
		given:
		model.required = true

		expect:
		render(template: '/forms/default/field', model: model) == '''<div class="fieldcontain  required">
	<label for="property">label<span class="required-indicator">*</span></label>
	widget
</div>'''
	}

}
