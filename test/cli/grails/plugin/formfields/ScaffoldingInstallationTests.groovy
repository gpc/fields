package grails.plugin.formfields

import org.junit.Test
import static org.apache.commons.io.FileUtils.checksumCRC32

class ScaffoldingInstallationTests extends AbstractTemporaryProjectTest {

	@Test
	void installTemplatesCopiesTemplatesToTargetApp() {
		runGrailsCommand "install-form-fields-templates"

		def srcFile = new File("src/templates/scaffolding/create.gsp")
		def destFile = new File(workDir, "src/templates/scaffolding/create.gsp")

		assert destFile.isFile()
		assert checksumCRC32(srcFile) == checksumCRC32(destFile)
	}

	@Override
	String getPluginName() {
		'form-fields'
	}

	@Override
	String getPluginVersion() {
		getClass().classLoader.loadClass("FormFieldsGrailsPlugin").newInstance().version
	}
}
