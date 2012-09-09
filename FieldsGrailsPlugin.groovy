/*
 * Copyright 2012 Rob Fletcher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import grails.plugin.formfields.BeanPropertyAccessorFactory
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator

class FieldsGrailsPlugin {

	def version = '1.3'
	def grailsVersion = '2.0 > *'
	def dependsOn = [:]
	def pluginExcludes = []

	def title = 'Fields Plugin'
	def author = 'Rob Fletcher'
	def authorEmail = 'rob@freeside.co'
	def description = 'Customizable form-field rendering based on overrideable GSP template'

	def documentation = 'http://freeside.co/grails-fields'
	def license = 'APACHE'
	def issueManagement = [system: 'GitHub', url: 'https://github.com/robfletcher/grails-fields/issues']
	def scm = [system: 'GitHub', url: 'https://github.com/robfletcher/grails-fields']

	def doWithSpring = {
		beanPropertyAccessorFactory(BeanPropertyAccessorFactory) {
			constraintsEvaluator = ref(ConstraintsEvaluator.BEAN_NAME)
			proxyHandler = ref('proxyHandler')
		}
	}

	def doWithApplicationContext = { appCtx ->
		def formFieldsTemplateService = appCtx.formFieldsTemplateService
		formFieldsTemplateService.ignoreControllerLookup = application.config.grails.plugins.formfields.ignoreControllerLookup
		formFieldsTemplateService.ignoreControllerActionLookup = application.config.grails.plugins.formfields.ignoreControllerActionLookup
		formFieldsTemplateService.ignoreBeanTypeLookup = application.config.grails.plugins.formfields.ignoreBeanTypeLookup
        formFieldsTemplateService.ignoreBeanSuperTypeLookup = application.config.grails.plugins.formfields.ignoreBeanSuperTypeLookup
        formFieldsTemplateService.ignoreAssociationLookup = application.config.grails.plugins.formfields.ignoreAssociationLookup
        formFieldsTemplateService.ignorePropertyTypeLookup = application.config.grails.plugins.formfields.ignorePropertyTypeLookup
		formFieldsTemplateService.ignorePropertySuperTypeLookup = application.config.grails.plugins.formfields.ignorePropertySuperTypeLookup
	}
}
