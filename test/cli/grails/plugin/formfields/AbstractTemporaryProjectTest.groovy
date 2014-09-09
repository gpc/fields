package grails.plugin.formfields

import grails.test.AbstractCliTestCase
import org.apache.commons.lang.RandomStringUtils
import org.junit.*

abstract class AbstractTemporaryProjectTest extends AbstractCliTestCase {

	/**
	 * @return the version string of the current plugin, e.g. `2.0.1`, `1.0.3-SNAPSHOT`, etc.
	 */
	abstract String getPluginVersion()

	/**
	 * @return the plugin name in hyphenated form as used in _BuildConfig.groovy_.
	 */
	abstract String getPluginName()

	protected void runGrailsCommand(String... args) {
		println "$workDir.canonicalPath > grails ${args.join(' ')}..."
		execute(args as List)
		waitForProcess()
		verifyHeader()
	}

	protected final File pluginDir = new File('.')
	private final File tempDir = new File(System.properties."java.io.tmpdir", getClass().simpleName)
	private String tempProjectName = RandomStringUtils.randomAlphanumeric(8)

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

	private void createTempApp() {
		workDir = tempDir
		runGrailsCommand 'create-app', tempProjectName
		def projectDir = new File(tempDir, tempProjectName)
		workDir = projectDir
		generateBuildConfig()
		runGrailsCommand '-refresh-dependencies clean'
		runGrailsCommand 'compile'
		runGrailsCommand 'package'
	}

	private void generateBuildConfig() {
		new File(workDir, "grails-app/conf/BuildConfig.groovy").text = """\
grails.servlet.version = "2.5"
grails.project.work.dir = "target"
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.source.level = 1.6
grails.project.target.level = 1.6

def grailsVersion = grails.util.Metadata.current.getGrailsVersion()

grails.project.dependency.resolution = {
    inherits "global"
    log "error"
    checksums true
    repositories {
        inherits true
        mavenLocal()
    }
    plugins {
        compile ":$pluginName:$pluginVersion"
        if(grailsVersion.startsWith("2.3.")) {
          compile ":scaffolding:2.0.3"
        } else if (grailsVersion.startsWith("2.4.")) {
          compile ":scaffolding:2.1.2"
        }
    }
}
"""
	}

}
