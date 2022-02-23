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

import groovy.transform.CompileDynamic
import nebula.plugin.info.InfoBrokerPlugin
import org.gradle.api.Project
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

/**
 * Simply writes out brokers values to a properties file.
 */
@CompileDynamic
class InfoPropertiesFile extends ConventionTask {
    private InfoBrokerPlugin infoBrokerPlugin

    @Inject
    InfoPropertiesFile(Project project) {
        infoBrokerPlugin = project.plugins.getPlugin(InfoBrokerPlugin) as InfoBrokerPlugin
    }

    @Input
    Map<String, ?> getManifest() {
        Map<String, String> entireMap = infoBrokerPlugin.buildNonChangingManifest()

        return entireMap
    }

    @OutputFile
    File propertiesFile

    @TaskAction
    void write() {
        PropertiesWriter.writeProperties(getPropertiesFile(), infoBrokerPlugin)
    }
}
