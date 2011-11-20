grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.source.level = 1.6
grails.project.target.level = 1.6

grails.project.dependency.resolution = {

    inherits "global"
    log "warn"

    repositories {
		grailsHome()
        grailsCentral()
		grailsPlugins()
        mavenCentral()
        mavenLocal()
    }

    dependencies {
    }

    plugins {
        build(":release:1.0.0.RC3") { export = false }
		test(":spock:0.6-SNAPSHOT") { export = false }
		test(":hibernate:$grailsVersion") { export = false }
    }

}
