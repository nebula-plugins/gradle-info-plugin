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
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Simply writes out brokers values to a properties file.
 */
class InfoPropertiesFile extends ConventionTask {

    @Input
    Map<String, ?> getManifest() {
        InfoBrokerPlugin manifestPlugin = project.plugins.getPlugin(InfoBrokerPlugin)

        def entireMap = manifestPlugin.buildNonChangingManifest()

        return entireMap
    }

    @OutputFile
    File propertiesFile

    @TaskAction
    def writeOut() {
        InfoBrokerPlugin basePlugin = project.plugins.getPlugin(InfoBrokerPlugin)

        // Gather all values, in contrast to buildNonChangingManifest
        def attrs = basePlugin.buildManifest()

        logger.info("Writing manifest values to ${getPropertiesFile()}")

        def manifestStr = attrs.collect { "${it.key}=${it.value}"}.join('\n')
        getPropertiesFile().text = manifestStr
    }
}
