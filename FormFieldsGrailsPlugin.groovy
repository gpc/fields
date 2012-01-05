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

class FormFieldsGrailsPlugin {

	def version = "1.0-SNAPSHOT"
	def grailsVersion = "2.0 > *"
	def dependsOn = [:]
	def pluginExcludes = [
			"grails-app/views/error.gsp"
	]

	def title = "Form Fields Plugin"
	def author = "Rob Fletcher"
	def authorEmail = "rfletcherEW@shortmail.com"
	def description = ""

	def documentation = "http://robfletcher.github.com/grails-form-fields"
	def license = "APACHE"
	def issueManagement = [system: "GitHub", url: "https://github.com/robfletcher/grails-form-fields/issues"]
	def scm = [url: "https://github.com/robfletcher/grails-form-fields"]

	def doWithSpring = {
		beanPropertyAccessorFactory(BeanPropertyAccessorFactory) {
			constraintsEvaluator = ref(ConstraintsEvaluator.BEAN_NAME)
		}
	}
	
}
