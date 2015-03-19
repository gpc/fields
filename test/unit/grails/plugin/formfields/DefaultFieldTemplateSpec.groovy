package grails.plugin.formfields

import grails.plugin.formfields.mock.Person
import grails.plugin.formfields.taglib.AbstractFormFieldsTagLibSpec
import grails.test.mixin.TestFor
import jodd.lagarto.dom.jerry.Jerry
import static jodd.lagarto.dom.jerry.Jerry.jerry

@TestFor(FormFieldsTagLib)
class DefaultFieldTemplateSpec extends AbstractFormFieldsTagLibSpec {
	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)
	Map model = [:]

	def setupSpec() {
		configurePropertyAccessorSpringBean()
	}

	def setup() {
		views["/_fields/default/_layout.gsp"] = """<g:set var="classes" value="fieldcontain "/>
			<g:if test="\${required}">
				<g:set var="classes" value="\${classes + 'required '}"/>
			</g:if>
			<g:if test="\${invalid}">
				<g:set var="classes" value="\${classes + 'error '}"/>
			</g:if>
			<div class="\${classes}">
				<label for="\${prefix}\${property}">\${label}<g:if test="\${required}"><span class="required-indicator">*</span></g:if></label>\${raw(renderedField)}
			</div>"""
		mockFormFieldsTemplateService.findTemplate(_, _, _) >> null
		mockFormFieldsTemplateService.findLayout(_, _, _) >> [path: '/_fields/default/layout']
		tagLib.formFieldsTemplateService = mockFormFieldsTemplateService
	}
	
	static Jerry $(String html) {
		jerry(html).children()
	}
	
	void "default rendering"() {
		given:
		def personInstance = new Person(
				name: "Clint Eastwood"
		)

		when:
		def output = applyTemplate('<f:field bean="personInstance" property="name" label="label" required="false"/>', [personInstance: personInstance])

		then:
		def root = $(output.toString())
		root.is('div.fieldcontain')

		and:
		def label = root.find('label')
		label.text() == 'label'
		label.attr('for') == 'name'
		
		and:
		label.next().is('input[name=name]')
	}

	void "container marked as invalid"() {
		given:
		def personInstance = new Person(
				name: "Clint Eastwood"
		)
		personInstance.errors.rejectValue('name', "error")

		when:
		def output = applyTemplate('<f:field bean="personInstance" property="name" label="label" required="false"/>', [personInstance: personInstance])

		then:
		$(output.toString()).hasClass('error')
	}

	void "container marked as required"() {
		given:
		def personInstance = new Person(
				name: "Clint Eastwood"
		)

		when:
		def output = applyTemplate('<f:field bean="personInstance" property="name" label="label" required="true"/>', [personInstance: personInstance])

		then:
		def root = $(output.toString())
		root.hasClass('required')
		
		and:
		def indicator = root.find('label .required-indicator')
		indicator.size()
		indicator.text() == '*'
	}

}
