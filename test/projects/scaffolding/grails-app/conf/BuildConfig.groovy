grails.servlet.version = "2.5"
grails.project.work.dir = "target"
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.7
grails.project.source.level = 1.7

grails.project.dependency.resolution = {
    inherits "global"
    log "error"
    checksums true

	def seleniumVersion = "2.26.0"
	def gebVersion = "0.9.0"

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
        test 'org.spockframework:spock-grails-support:0.7-groovy-2.0'
		test "org.gebish:geb-spock:$gebVersion"
		test "org.seleniumhq.selenium:selenium-support:$seleniumVersion"
		test "org.seleniumhq.selenium:selenium-htmlunit-driver:$seleniumVersion"
//		test "org.seleniumhq.selenium:selenium-chrome-driver:$seleniumVersion"
    }
    plugins {
        compile ":hibernate:$grailsVersion"
        compile ":jquery:1.7.2"
        compile ":resources:1.1.6"
        build ":tomcat:$grailsVersion"
        test(':spock:0.7') {
            exclude 'spock-grails-support'
        }
		test ":geb:$gebVersion"
    }
}

grails.plugin.location.'fields' = '../../..'
