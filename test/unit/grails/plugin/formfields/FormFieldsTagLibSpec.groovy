package grails.plugin.formfields

import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import grails.plugin.formfields.mock.*
import grails.test.mixin.*
import spock.lang.*

@TestFor(FormFieldsTagLib)
@Mock([Person, Employee])
class FormFieldsTagLibSpec extends Specification {

	def personInstance
	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

	def setupSpec() {
		defineBeans {
			constraintsEvaluator(DefaultConstraintEvaluator)
			beanPropertyAccessorFactory(BeanPropertyAccessorFactory) {
				constraintsEvaluator = ref('constraintsEvaluator')
			}
		}
	}

	def setup() {
		def taglib = applicationContext.getBean(FormFieldsTagLib)
		
		mockFormFieldsTemplateService.findTemplate(_, 'field') >> [path: '/_fields/default/field']
		taglib.formFieldsTemplateService = mockFormFieldsTemplateService

		// mock up a sitemesh layout call
		taglib.metaClass.applyLayout = { Map attrs, Closure body ->
			if (attrs.name == '_fields/embedded') {
				out << '<fieldset class="embedded ' << attrs.params.type << '">'
				out << '<legend>' << attrs.params.legend << '</legend>'
				out << body()
				out << '</fieldset>'
			}
		}

		personInstance = new Person(name: "Bart Simpson", password: "bartman", gender: Gender.Male, dateOfBirth: new Date(87, 3, 19), minor: true)
		personInstance.address = new Address(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")
	}

	def cleanup() {
		views.clear()
		applicationContext.getBean("groovyPagesTemplateEngine").clearPageCache()
		applicationContext.getBean("groovyPagesTemplateRenderer").clearCache()

		messageSource.@messages.clear() // bit of a hack but messages don't get torn down otherwise
	}

	void 'f:input requires a bean attribute'() {
		when:
		applyTemplate('<f:input property="name"/>')

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
		views["/_fields/default/_field.gsp"] = '${bean.getClass().simpleName}'

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
		views["/_fields/default/_field.gsp"] = '${bean.getClass().simpleName}.${property}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "Person.name"
	}

	void "constraints are passed to template"() {
		given:
		views["/_fields/default/_field.gsp"] = 'nullable=${constraints.nullable}, blank=${constraints.blank}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "nullable=false, blank=false"
	}

	void "label is resolved by convention and passed to template"() {
		given:
		views["/_fields/default/_field.gsp"] = '<label>${label}</label>'

		and:
		messageSource.addMessage("Person.name.label", request.locale, "Name of person")

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
	}

	void "label is defaulted to natural property name"() {
		given:
		views["/_fields/default/_field.gsp"] = '<label>${label}</label>'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<label>Name</label>"
		applyTemplate('<f:field bean="personInstance" property="dateOfBirth"/>', [personInstance: personInstance]) == "<label>Date Of Birth</label>"
	}

	void "label can be overridden by label attribute"() {
		given:
		views["/_fields/default/_field.gsp"] = '<label>${label}</label>'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name" label="Name of person"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
	}

	void "label can be overridden by label key attribute"() {
		given:
		views["/_fields/default/_field.gsp"] = '<label>${label}</label>'

		and:
		messageSource.addMessage("custom.name.label", request.locale, "Name of person")

		expect:
		applyTemplate('<f:field bean="personInstance" property="name" label="custom.name.label"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
	}

	void "value is defaulted to property value"() {
		given:
		views["/_fields/default/_field.gsp"] = '<g:formatDate date="${value}" format="yyyy-MM-dd"/>'

		expect:
		applyTemplate('<f:field bean="personInstance" property="dateOfBirth"/>', [personInstance: personInstance]) == "1987-04-19"
	}

	void "value is overridden by value attribute"() {
		given:
		views["/_fields/default/_field.gsp"] = '${value}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name" value="Bartholomew J. Simpson"/>', [personInstance: personInstance]) == "Bartholomew J. Simpson"
	}

	void "value falls back to default"() {
		given:
		views["/_fields/default/_field.gsp"] = '${value}'

		and:
		personInstance.name = null

		expect:
		applyTemplate('<f:field bean="personInstance" property="name" default="A. N. Other"/>', [personInstance: personInstance]) == "A. N. Other"
	}

	void "default attribute is ignored if property has non-null value"() {
		given:
		views["/_fields/default/_field.gsp"] = '${value}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name" default="A. N. Other"/>', [personInstance: personInstance]) == "Bart Simpson"
	}

	void "errors passed to template is an empty collection for valid bean"() {
		given:
		views["/_fields/default/_field.gsp"] = '<g:each var="error" in="${errors}"><em>${error}</em></g:each>'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == ""
	}

	void "errors passed to template is a collection of strings"() {
		given:
		views["/_fields/default/_field.gsp"] = '<g:each var="error" in="${errors}"><em>${error}</em></g:each>'

		and:
		personInstance.errors.rejectValue("name", "blank")
		personInstance.errors.rejectValue("name", "nullable")

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<em>blank</em><em>nullable</em>"
	}

	void "required flag is passed to template"() {
		given:
		views["/_fields/default/_field.gsp"] = 'required=${required}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "required=true"
	}

	void "required flag can be forced with attribute"() {
		given:
		views["/_fields/default/_field.gsp"] = 'required=${required}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="minor" required="true"/>', [personInstance: personInstance]) == "required=true"
	}

	void "required flag can be forced off with attribute"() {
		given:
		views["/_fields/default/_field.gsp"] = 'required=${required}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name" required="false"/>', [personInstance: personInstance]) == "required=false"
	}

	void "invalid flag is passed to template if bean has errors"() {
		given:
		views["/_fields/default/_field.gsp"] = 'invalid=${invalid}'

		and:
		personInstance.errors.rejectValue("name", "blank")

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "invalid=true"
	}

	void "invalid flag is not passed to template if bean has no errors"() {
		given:
		views["/_fields/default/_field.gsp"] = 'invalid=${invalid}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == "invalid=false"
	}

	void "invalid flag can be overridden with attribute"() {
		given:
		views["/_fields/default/_field.gsp"] = 'invalid=${invalid}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name" invalid="true"/>', [personInstance: personInstance]) == "invalid=true"
	}

	void "rendered input is passed to template"() {
		given:
		views["/_fields/default/_field.gsp"] = '${widget}'

		expect:
		applyTemplate('<f:field bean="personInstance" property="name"/>', [personInstance: personInstance]) == '<input type="text" name="name" value="Bart Simpson" required="" id="name" />'
	}

    @Issue('https://github.com/robfletcher/grails-fields/pull/16')
    void 'tag body can be used instead of the input'() {
        given:
        views['/_fields/default/_field.gsp'] = '${widget}'

        expect:
        applyTemplate('<f:field bean="personInstance" property="name">BODY</f:field>', [personInstance: personInstance]) == 'BODY'
    }

    @Issue('https://github.com/robfletcher/grails-fields/pull/16')
    void 'the model is passed to a tag body if there is one'() {
        given:
        views['/_fields/default/_field.gsp'] = '${widget}'

        expect:
        applyTemplate('<f:field bean="personInstance" property="name">bean: ${bean.getClass().simpleName}, property: ${property}, type: ${type.simpleName}, label: ${label}, value: ${value}</f:field>', [personInstance: personInstance]) == 'bean: Person, property: name, type: String, label: Name, value: Bart Simpson'
    }

	void "all tag renders fields for all properties"() {
		given:
		views["/_fields/default/_field.gsp"] = '${property} '

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
		views["/_fields/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:all bean="personInstance"/>', [personInstance: personInstance])

		then:
		output.contains('address.street address.city address.country')
	}

	void "all tag wraps embedded properties in a container"() {
		given:
		views["/_fields/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:all bean="personInstance"/>', [personInstance: personInstance])

		then:
		output.contains('<fieldset class="embedded address"><legend>Address</legend>address.street address.city address.country </fieldset>')
	}

	@Unroll({"all tag skips $property property"})
	void 'all tag skips excluded properties'() {
		given:
		views["/_fields/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:all bean="personInstance"/>', [personInstance: personInstance])

		then:
		!output.contains('excludedProperty')

		where:
		property << ['id', 'version', 'onLoad', 'lastUpdated', 'excludedProperty']
	}

	@Issue('https://github.com/robfletcher/grails-form-fields/issues/12')
	void 'all tag skips properties listed with the except attribute'() {
		given:
		views["/_fields/default/_field.gsp"] = '${property} '

		when:
		def output = applyTemplate('<f:all bean="personInstance" except="password, minor"/>', [personInstance: personInstance])

		then:
		!output.contains('password')
		!output.contains('minor')
	}

	@Issue('https://github.com/robfletcher/grails-form-fields/issues/13')
	void 'bean attribute does not have to be specified if it is in scope from f:with'() {
		given:
		views["/_fields/default/_field.gsp"] = '${property} '

		expect:
		applyTemplate('<f:with bean="personInstance"><f:field property="name"/></f:with>', [personInstance: personInstance]) == 'name '
	}

	@Issue('https://github.com/robfletcher/grails-form-fields/issues/13')
	void 'scoped bean attribute does not linger around after f:with tag'() {
		expect:
		applyTemplate('<f:with bean="personInstance">${pageScope.getVariable("f:with:bean")}</f:with>${pageScope.getVariable("f:with:bean")}', [personInstance: personInstance]) == 'Bart Simpson'
	}

	@Issue('https://github.com/robfletcher/grails-form-fields/pull/16')
	void 'f:field can work without a bean attribute'() {
		given:
		views["/_fields/default/_field.gsp"] = '${property}'

		expect:
		applyTemplate('<f:field property="name"/>') == 'name'
	}

	@Issue('https://github.com/robfletcher/grails-form-fields/pull/16')
	void 'label is the natural property name if there is no bean attribute'() {
		given:
		views["/_fields/default/_field.gsp"] = '${label}'

		expect:
		applyTemplate('<f:field property="name"/>') == 'Name'
	}

    @Issue('https://github.com/robfletcher/grails-form-fields/pull/17')
    void 'arbitrary attributes can be passed to the field template'() {
        given:
        views["/_fields/default/_field.gsp"] = '${foo}'

        expect:
        applyTemplate('<f:field bean="personInstance" property="name" foo="bar"/>', [personInstance: personInstance]) == 'bar'
    }

    @Ignore // breaks because of GRAILS-8700
    @Issue('https://github.com/robfletcher/grails-form-fields/pull/17')
    void 'arbitrary attributes on f:field are not passed to the input template'() {
        given:
        views["/_fields/default/_field.gsp"] = '${widget}'
        views["/_fields/person/name/_input.gsp"] = '<span>${foo}</span>'

		and:
		mockFormFieldsTemplateService.findTemplate(_, 'input') >> [path: '/_fields/person/name/input']

        expect:
        applyTemplate('<f:field bean="personInstance" property="name" foo="bar"/>', [personInstance: personInstance]) == '<span></span>'
    }

    @Issue('https://github.com/robfletcher/grails-form-fields/pull/17')
    void 'arbitrary attributes on f:field are not passed to the default input'() {
        given:
        views["/_fields/default/_field.gsp"] = '${widget}'

        expect:
        applyTemplate('<f:field bean="personInstance" property="name" foo="bar"/>', [personInstance: personInstance]) == '<input type="text" name="name" value="Bart Simpson" required="" id="name" />'
    }

	@Issue('https://github.com/robfletcher/grails-form-fields/pull/17')
	void 'arbitrary attributes on f:input are passed to the input template'() {
		given:
		views["/_fields/person/name/_input.gsp"] = '${foo}'

		and:
		mockFormFieldsTemplateService.findTemplate(_, 'input') >> [path: '/_fields/person/name/input']

		expect:
		applyTemplate('<f:input bean="personInstance" property="name" foo="bar"/>', [personInstance: personInstance]) == 'bar'
	}

	@Issue('https://github.com/robfletcher/grails-form-fields/pull/17')
	void 'arbitrary attributes on f:input are passed to the default input'() {
		expect:
		applyTemplate('<f:input bean="personInstance" property="name" foo="bar"/>', [personInstance: personInstance]) == '<input type="text" name="name" value="Bart Simpson" required="" foo="bar" id="name" />'
	}

}
