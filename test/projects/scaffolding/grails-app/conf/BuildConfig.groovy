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

    repositories {
        inherits true
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenLocal()
        mavenCentral()
    }
    dependencies {
    }
    plugins {
        compile ":hibernate:$grailsVersion"
        compile ":jquery:1.7"
        compile ":resources:1.1.3"
        build ":tomcat:$grailsVersion"
    }
}

grails.plugin.location.'form-fields' = '../../..'
