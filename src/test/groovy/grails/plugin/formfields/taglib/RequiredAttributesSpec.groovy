package grails.plugin.formfields.taglib

import grails.plugin.formfields.mock.Person
import grails.plugin.formfields.*
import grails.test.mixin.*
import org.grails.taglib.GrailsTagException

@TestFor(FormFieldsTagLib)
@Mock(Person)
class RequiredAttributesSpec extends AbstractFormFieldsTagLibSpec {

	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

	def setupSpec() {
		configurePropertyAccessorSpringBean()
	}

	def setup() {
		def taglib = applicationContext.getBean(FormFieldsTagLib)

		mockFormFieldsTemplateService.findTemplate(_, 'wrapper', null) >> [path: '/_fields/default/wrapper']
        mockFormFieldsTemplateService.getTemplateFor('wrapper') >> "wrapper"
        mockFormFieldsTemplateService.getTemplateFor('widget') >> "widget"
        mockFormFieldsTemplateService.getTemplateFor('displayWrapper') >> "displayWrapper"
        mockFormFieldsTemplateService.getTemplateFor('displayWidget') >> "displayWidget"
		taglib.formFieldsTemplateService = mockFormFieldsTemplateService
	}

	void 'f:input requires a bean attribute'() {
		when:
		applyTemplate('<f:input property="name"/>')

		then:
		thrown GrailsTagException
	}

	void 'f:displayWidget requires a bean attribute'() {
		when:
		applyTemplate('<f:displayWidget property="name"/>')

		then:
		thrown GrailsTagException
	}

	void "property attribute is required"() {
		when:
		applyTemplate('<f:field bean="${personInstance}"/>', [personInstance: personInstance])

		then:
		thrown GrailsTagException
	}

	void 'if f:field is supplied a bean attribute it must not be null'() {
		when:
		applyTemplate('<f:field bean="${personInstance}" property="name"/>', [personInstance: null])

		then:
		thrown GrailsTagException
	}

	void "bean attribute can be a String"() {
		given:
		views["/_fields/default/_wrapper.gsp"] = '${bean.getClass().simpleName}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "Person"
	}

	void "bean attribute string must refer to variable in page"() {
		when:
		applyTemplate('<f:field bean="personInstance" property="name"/>')

		then:
		thrown GrailsTagException
	}

}
