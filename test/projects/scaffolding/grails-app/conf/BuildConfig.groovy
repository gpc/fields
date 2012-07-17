grails.servlet.version = "2.5"
grails.project.work.dir = "target"
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
grails.project.source.level = 1.6

grails.project.dependency.resolution = {
    inherits "global"
    log "error"
    checksums true

	def seleniumVersion = "2.24.1"
	def gebVersion = "0.7.0"

    repositories {
        inherits true
        grailsPlugins()
        grailsHome()
        grailsCentral()
		grailsRepo "http://grails.org/plugins"
        mavenLocal()
        mavenCentral()
		mavenRepo "https://nexus.codehaus.org/content/repositories/snapshots"
    }
    dependencies {
		test "org.codehaus.geb:geb-spock:$gebVersion"
		test "org.seleniumhq.selenium:selenium-support:$seleniumVersion"
		test "org.seleniumhq.selenium:selenium-htmlunit-driver:$seleniumVersion"
    }
    plugins {
        compile ":hibernate:$grailsVersion"
        compile ":jquery:1.7.2"
        compile ":resources:1.1.6"
        build ":tomcat:$grailsVersion"
		test ":spock:0.6"
		test ":geb:$gebVersion"
    }
}

grails.plugin.location.'fields' = '../../..'
