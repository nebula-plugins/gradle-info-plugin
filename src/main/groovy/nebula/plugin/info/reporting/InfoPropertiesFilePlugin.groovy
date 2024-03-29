/*
 * Copyright 2014-2019 Netflix, Inc.
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

package nebula.plugin.info.reporting

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoReporterPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.tasks.TaskProvider

/**
 * Write contents of the manifest to a file, as a property file.
 */
class InfoPropertiesFilePlugin implements Plugin<Project>, InfoReporterPlugin {

    TaskProvider<InfoPropertiesFile> manifestTask

    void apply(Project project) {
        project.plugins.withType(InfoBrokerPlugin).configureEach {  InfoBrokerPlugin basePlugin ->

            manifestTask = project.tasks.register('writeManifestProperties', InfoPropertiesFile) { task ->
                if (project.plugins.hasPlugin(BasePlugin)) {
                    BasePluginExtension baseExtension = project.extensions.getByType(BasePluginExtension)
                    task.propertiesFile.set(project.layout.buildDirectory.file("manifest/${baseExtension.archivesName.get()}.properties"))
                } else {
                    task.propertiesFile.set(project.layout.buildDirectory.file("manifest/info.properties"))
                }

            }
        }
    }

    TaskProvider<InfoPropertiesFile> getManifestTask() {
        assert manifestTask != null, 'InfoPropertiesFilePlugin has to be applied first'
        return manifestTask
    }
}
