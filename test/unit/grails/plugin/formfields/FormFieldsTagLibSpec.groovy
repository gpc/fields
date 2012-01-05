package grails.plugin.formfields

import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import grails.test.mixin.*
import grails.plugin.formfields.mock.*
import spock.lang.*

@TestFor(FormFieldsTagLib)
@Mock([Person, Employee])
class FormFieldsTagLibSpec extends Specification {

	def personInstance

	def setupSpec() {
		defineBeans {
			constraintsEvaluator(DefaultConstraintEvaluator)
			beanPropertyAccessorFactory(BeanPropertyAccessorFactory) {
				constraintsEvaluator = ref('constraintsEvaluator')
			}
		}
	}

	def setup() {
		def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)
		mockFormFieldsTemplateService.findTemplate(_, 'field') >> [path: '/forms/default/field']
		applicationContext.getBean(FormFieldsTagLib).formFieldsTemplateService = mockFormFieldsTemplateService

		personInstance = new Person(name: "Bart Simpson", password: "bartman", gender: Gender.Male, dateOfBirth: new Date(87, 3, 19), minor: true)
		personInstance.address = new Address(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")
	}

	def cleanup() {
		views.clear()
		applicationContext.getBean("groovyPagesTemplateEngine").clearPageCache()
		applicationContext.getBean("groovyPagesTemplateRenderer").clearCache()

		messageSource.@messages.clear() // bit of a hack but messages don't get torn down otherwise
	}

	void "bean attribute is required"() {
		when:
		applyTemplate('<f:field property="name"/>')

		then:
		thrown GrailsTagException
	}

	void "property attribute is required"() {
		when:
		applyTemplate('<f:field bean="${personInstance}"/>', [personInstance: personInstance])

		then:
		thrown GrailsTagException
	}

	void "bean attribute must not be null"() {
		when:
		applyTemplate('<f:field bean="${personInstance}" property="name"/>', [personInstance: null])

		then:
		thrown GrailsTagException
	}

	void "bean attribute can be a String"() {
		given:
		views["/forms/default/_field.gsp"] = '${bean.getClass().simpleName}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "Person"
	}

	void "bean attribute string must refer to variable in page"() {
		when:
		applyTemplate('<f:field bean="personInstance" property="name"/>')

		then:
		thrown GrailsTagException
	}

	void "bean and property attributes are passed to template"() {
		given:
		views["/forms/default/_field.gsp"] = '${bean.getClass().simpleName}.${property}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "Person.name"
	}

	void "constraints are passed to template"() {
		given:
		views["/forms/default/_field.gsp"] = 'nullable=${constraints.nullable}, blank=${constraints.blank}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "nullable=false, blank=false"
	}

	void "label is resolved by convention and passed to template"() {
		given:
		views["/forms/default/_field.gsp"] = '<label>${label}</label>'

		and:
		messageSource.addMessage("Person.name.label", request.locale, "Name of person")

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
	}

	void "label is defaulted to natural property name"() {
		given:
		views["/forms/default/_field.gsp"] = '<label>${label}</label>'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<label>Name</label>"
		applyTemplate('<f:field bean="personInstance" property="dateOfBirth"/>', [personInstance: personInstance]) == "<label>Date Of Birth</label>"
	}

	void "label can be overridden by label attribute"() {
		given:
		views["/forms/default/_field.gsp"] = '<label>${label}</label>'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name" label="Name of person"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
	}

	void "label can be overridden by label key attribute"() {
		given:
		views["/forms/default/_field.gsp"] = '<label>${label}</label>'

		and:
		messageSource.addMessage("custom.name.label", request.locale, "Name of person")

		expect:
		applyTemplate('<f:field bean="personInstance" property="name" labelKey="custom.name.label"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
	}

	void "value is defaulted to property value"() {
		given:
		views["/forms/default/_field.gsp"] = '<g:formatDate date="${value}" format="yyyy-MM-dd"/>'

		expect:
		applyTemplate('<f:field bean="personInstance" property="dateOfBirth"/>', [personInstance: personInstance]) == "1987-04-19"
	}

	void "value is overridden by value attribute"() {
		given:
		views["/forms/default/_field.gsp"] = '${value}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name" value="Bartholomew J. Simpson"/>', [personInstance: personInstance]) == "Bartholomew J. Simpson"
	}

	void "value falls back to default"() {
		given:
		views["/forms/default/_field.gsp"] = '${value}'

		and:
		personInstance.name = null

		expect:
		applyTemplate('<f:field bean="personInstance" property="name" default="A. N. Other"/>', [personInstance: personInstance]) == "A. N. Other"
	}

	void "default attribute is ignored if property has non-null value"() {
		given:
		views["/forms/default/_field.gsp"] = '${value}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name" default="A. N. Other"/>', [personInstance: personInstance]) == "Bart Simpson"
	}

	void "errors passed to template is an empty collection for valid bean"() {
		given:
		views["/forms/default/_field.gsp"] = '<g:each var="error" in="${errors}"><em>${error}</em></g:each>'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == ""
	}

	void "errors passed to template is a collection of strings"() {
		given:
		views["/forms/default/_field.gsp"] = '<g:each var="error" in="${errors}"><em>${error}</em></g:each>'

		and:
		personInstance.errors.rejectValue("name", "blank")
		personInstance.errors.rejectValue("name", "nullable")

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<em>blank</em><em>nullable</em>"
	}

	void "required flag is passed to template"() {
		given:
		views["/forms/default/_field.gsp"] = 'required=${required}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "required=true"
	}

	void "required flag can be forced with attribute"() {
		given:
		views["/forms/default/_field.gsp"] = 'required=${required}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="minor" required="true"/>', [personInstance: personInstance]) == "required=true"
	}

	void "required flag can be forced off with attribute"() {
		given:
		views["/forms/default/_field.gsp"] = 'required=${required}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name" required="false"/>', [personInstance: personInstance]) == "required=false"
	}

	void "invalid flag is passed to template if bean has errors"() {
		given:
		views["/forms/default/_field.gsp"] = 'invalid=${invalid}'

		and:
		personInstance.errors.rejectValue("name", "blank")

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "invalid=true"
	}

	void "invalid flag is not passed to template if bean has no errors"() {
		given:
		views["/forms/default/_field.gsp"] = 'invalid=${invalid}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "invalid=false"
	}

	void "invalid flag can be overridden with attribute"() {
		given:
		views["/forms/default/_field.gsp"] = 'invalid=${invalid}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name" invalid="true"/>', [personInstance: personInstance]) == "invalid=true"
	}

	void "rendered input is passed to template"() {
		given:
		views["/forms/default/_field.gsp"] = '${widget}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == '<input type="text" name="name" value="Bart Simpson" required="" id="name" />'
	}

	void "all tag renders fields for all properties"() {
		given:
		views["/forms/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:all bean="personInstance"/>', [personInstance: personInstance])

		then:
		output =~ /\bname\b/
		output =~ /\bpassword\b/
		output =~ /\bgender\b/
		output =~ /\bdateOfBirth\b/
		output =~ /\bminor\b/
	}

	void "all tag renders individual fields for embedded properties"() {
		given:
		views["/forms/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:all bean="personInstance"/>', [personInstance: personInstance])

		then:
		output.contains('address.street address.city address.country')
	}

	void "all tag wraps embedded properties in a container"() {
		given:
		views["/forms/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:all bean="personInstance"/>', [personInstance: personInstance])

		then:
		output.contains('<fieldset class="address"><legend>Address</legend>address.street address.city address.country </fieldset>')
	}

	@Unroll({"all tag skips $property property"})
	void 'all tag skips excluded properties'() {
		given:
		views["/forms/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:all bean="personInstance"/>', [personInstance: personInstance])

		then:
		!output.contains('excludedProperty')

		where:
		property << ['id', 'version', 'onLoad', 'lastUpdated', 'excludedProperty']
	}

}
