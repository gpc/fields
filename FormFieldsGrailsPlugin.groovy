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

}
