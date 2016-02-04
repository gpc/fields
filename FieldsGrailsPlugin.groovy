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
import grails.plugin.formfields.LegacyGrailsSupport
import org.codehaus.groovy.grails.validation.ConstraintsEvaluator

class FieldsGrailsPlugin {

	def version = '1.6-SNAPSHOT'
	def grailsVersion = '2.0 > *'
	def dependsOn = [:]
	def pluginExcludes = []

	def title = 'Fields Plugin'
	def author = 'Rob Fletcher'
	def authorEmail = 'rob@freeside.co'
	def description = 'Customizable form-field rendering based on overrideable GSP template'

	def documentation = 'https://grails-fields-plugin.github.io/grails-fields'
	def license = 'APACHE'
	def issueManagement = [system: 'GitHub', url: 'https://github.com/grails-fields-plugin/grails-fields/issues']
	def scm = [system: 'GitHub', url: 'https://github.com/grails-fields-plugin/grails-fields']
    def organization = [ name: "Grails Fields Plugin Developers Group", url: "https://github.com/grails-fields-plugin" ]

	def developers = [
		[ name: "Rob Fletcher", email: "rob@freeside.co" ],
		[ name: "Erik Pragt", email: "erik.pragt@gmail.com" ],
		[ name: "Dónal Murtagh", email: "domurtag@yahoo.co.uk" ],
		[ name: "Craig Burke", email: "craig@craigburke.com" ],
		[ name: "Soeren Glasius", email: "soeren@glasius.dk" ],
		[ name: "Martín Pablo Caballero", email: "mpccolorado@gmail.com" ],
		[ name: "Tom Crossland", email: "tom.crossland@gmail.com" ]
	]


	def doWithSpring = {
		beanPropertyAccessorFactory(BeanPropertyAccessorFactory) {
			constraintsEvaluator = ref(ConstraintsEvaluator.BEAN_NAME)
			proxyHandler = ref('proxyHandler')
		}
	}

	def doWithApplicationContext = { ctx ->
		if (LegacyGrailsSupport.supportedLegacyGrailsVersion) {
			def formFieldsTagLib = ctx.getBean('grails.plugin.formfields.FormFieldsTagLib')
			LegacyGrailsSupport.addRawMethodImplementation(formFieldsTagLib)
		}
	}

}
