package org.codehaus.groovy.grails.plugins.beanfields

import grails.test.AbstractCliTestCase
import org.apache.commons.lang.RandomStringUtils
import static org.apache.commons.io.FileUtils.checksumCRC32
import org.junit.*

class ScaffoldingInstallationTests extends AbstractCliTestCase {

	File tempDir = new File(System.properties."java.io.tmpdir", getClass().simpleName)
	String tempProjectName = RandomStringUtils.randomAlphanumeric(8)

	/*
	 * only want to install plugin once as its slow but can't do statically from @BeforeClass as runGrailsCommand is an
	 * instance method (wish this was Spock where setupSpec would do the job perfectly).
	 */
	private static boolean mavenInstalled = false

	@Before
	void setUp() {
		super.setUp()
		if (!mavenInstalled) {
			runGrailsCommand 'maven-install'
			mavenInstalled = true
		}
		tempDir.mkdirs()
		createTempApp()
	}

	@After
	void tearDown() {
		super.tearDown()
		tempDir.deleteDir()
	}

	@Test
	void installTemplatesCopesTemplatesToTargetApp() {
		runGrailsCommand "install-form-fields-templates"

		def srcFile = new File("src/templates/scaffolding/create.gsp")
		def destFile = new File(workDir, "src/templates/scaffolding/create.gsp")

		assert destFile.isFile()
		assert checksumCRC32(srcFile) == checksumCRC32(destFile)
	}

	private void createTempApp() {
		workDir = tempDir
		runGrailsCommand "create-app", tempProjectName
		def projectDir = new File(tempDir, tempProjectName)
		workDir = projectDir
		generateBuildConfig()
		runGrailsCommand "package"
	}

	private void runGrailsCommand(String... args) {
		println "$workDir.absolutePath > `grails ${args.join(' ')}`..."
		execute(args as List)
		waitForProcess()
		verifyHeader()
	}

	private void generateBuildConfig() {
		new File(workDir, "grails-app/conf/BuildConfig.groovy").text = """\
grails.servlet.version = "2.5"
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.source.level = 1.6
grails.project.target.level = 1.6

grails.project.dependency.resolution = {
    inherits "global"
    log "error"
    checksums true
    repositories {
        inherits true
		mavenLocal()
    }
    plugins {
		compile ":form-fields:${pluginVersion}"
    }
}
"""
	}

	private getPluginVersion() {
		getClass().classLoader.loadClass("FormFieldsGrailsPlugin").newInstance().version
	}
}
