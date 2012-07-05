package grails.plugin.formfields

import org.junit.Test
import static org.apache.commons.io.FileUtils.checksumCRC32

class ScaffoldingInstallationTests extends AbstractTemporaryProjectTest {

    static TEMPLATES = ['create.gsp', 'edit.gsp', 'show.gsp']

    @Test
	void installTemplatesCopiesTemplatesToTargetApp() {
		runGrailsCommand "install-form-fields-templates"

		for (filename in TEMPLATES) {
			def srcFile = new File(pluginDir, "src/templates/scaffolding/$filename")
			def targetFile = new File(workDir, "src/templates/scaffolding/$filename")

			assert targetFile.isFile()
			assert checksumCRC32(srcFile) == checksumCRC32(targetFile)
		}
		
		for (filename in ['list.gsp', 'renderEditor.template']) {
			assert !new File(workDir, "src/templates/scaffolding/$filename").exists()
		}
	}

	@Test
	void installTemplatesOverwritesDefaultScaffoldingTemplates() {
		runGrailsCommand "install-templates"

		for (filename in TEMPLATES) {
			def srcFile = new File(pluginDir, "src/templates/scaffolding/$filename")
			def targetFile = new File(workDir, "src/templates/scaffolding/$filename")

			assert targetFile.isFile()
			assert checksumCRC32(srcFile) != checksumCRC32(targetFile)
		}

		runGrailsCommand "install-form-fields-templates"

		for (filename in TEMPLATES) {
			def srcFile = new File(pluginDir, "src/templates/scaffolding/$filename")
			def targetFile = new File(workDir, "src/templates/scaffolding/$filename")

			assert targetFile.isFile()
			assert checksumCRC32(srcFile) == checksumCRC32(targetFile)
		}
	}

	@Override
	String getPluginName() {
		'fields'
	}

	@Override
	String getPluginVersion() {
		getClass().classLoader.loadClass("FieldsGrailsPlugin").newInstance().version
	}
}
