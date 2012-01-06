/*
 * Copyright 2012 Rob Fletcher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

includeTargets << grailsScript("_GrailsInit")

target(installScaffoldingTemplates: "Installs scaffolding templates that use f:all to render properties") {

	def srcdir = new File("$fieldsPluginDir/src/templates/scaffolding")
	def destdir = new File("$basedir/src/templates/scaffolding/")

	if (srcdir?.isDirectory()) {
		event "StatusUpdate", ["Copying templates from $fieldsPluginDir"]

		for (name in fieldsPluginDir.listFiles()) {
			ant.copy(todir: destdir, overwrite: true, failonerror: false) {
				fileset dir: srcdir, includes: '*.gsp'
			}
		}
		event "StatusFinal", ["Template installation complete"]
	} else {
		event "StatusError", ["Unable to install templates as plugin template files are missing"]
	}
}

setDefaultTarget(installScaffoldingTemplates)