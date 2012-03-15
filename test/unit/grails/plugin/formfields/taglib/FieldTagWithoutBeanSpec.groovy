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
class FieldTagWithoutBeanSpec extends AbstractFormFieldsTagLibSpec {

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

	void 'f:field can work without a bean attribute'() {
		given:
		views["/_fields/default/_field.gsp"] = '${property}'

		expect:
		applyTemplate('<f:field property="name"/>') == 'name'
	}

	void 'label is the natural property name if there is no bean attribute'() {
		given:
		views["/_fields/default/_field.gsp"] = '${label}'

		expect:
		applyTemplate('<f:field property="name"/>') == 'Name'
	}

}