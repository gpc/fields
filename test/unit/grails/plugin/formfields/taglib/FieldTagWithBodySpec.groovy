package grails.plugin.formfields.taglib

import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.validation.DefaultConstraintEvaluator
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import grails.plugin.formfields.*
import grails.plugin.formfields.mock.*
import grails.test.mixin.*
import spock.lang.*

@Issue('https://github.com/robfletcher/grails-fields/pull/16')
@TestFor(FormFieldsTagLib)
@Mock(Person)
class FieldTagWithBodySpec extends AbstractFormFieldsTagLibSpec {

	def mockFormFieldsTemplateService = Mock(FormFieldsTemplateService)

	def setupSpec() {
		defineBeans {
			constraintsEvaluator(DefaultConstraintEvaluator)
			beanPropertyAccessorFactory(BeanPropertyAccessorFactory) {
				constraintsEvaluator = ref('constraintsEvaluator')
				proxyHandler = new DefaultProxyHandler()
			}
		}
	}

	def setup() {
		def taglib = applicationContext.getBean(FormFieldsTagLib)

		mockFormFieldsTemplateService.findTemplate(_, 'field') >> [path: '/_fields/default/field']
		taglib.formFieldsTemplateService = mockFormFieldsTemplateService
	}

    void 'tag body can be used instead of the input'() {
        given:
        views['/_fields/default/_field.gsp'] = '${widget}'

        expect:
        applyTemplate('<f:field bean="personInstance" property="name">BODY</f:field>', [personInstance: personInstance]) == 'BODY'
    }

    void 'the model is passed to a tag body if there is one'() {
        given:
        views['/_fields/default/_field.gsp'] = '${widget}'

        expect:
        applyTemplate('<f:field bean="personInstance" property="name">bean: ${bean.getClass().simpleName}, property: ${property}, type: ${type.simpleName}, label: ${label}, value: ${value}</f:field>', [personInstance: personInstance]) == 'bean: Person, property: name, type: String, label: Name, value: Bart Simpson'
    }

}