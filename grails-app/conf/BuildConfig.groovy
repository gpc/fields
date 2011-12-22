grails.servlet.version = "2.5"
grails.project.work.dir = "target"
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.source.level = 1.6
grails.project.target.level = 1.6

grails.project.dependency.resolution = {

    inherits "global"
    log "warn"

    repositories {
		inherits true
		grailsHome()
        grailsCentral()
		grailsPlugins()
        mavenCentral()
        mavenLocal()
    }

    dependencies {
		test 'javassist:javassist:3.12.0.GA'
		test 'org.jodd:jodd-wot:3.3'
    }

    plugins {
        build(":release:1.0.0.RC3") { export = false }
		test(":spock:0.6-SNAPSHOT") { export = false }
    }

}
