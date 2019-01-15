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
import nebula.plugin.info.java.InfoJavaPlugin
import nebula.test.ProjectSpec
import org.gradle.api.plugins.JavaPlugin

class InfoPropertiesFilePluginSpec extends ProjectSpec {

    def 'ensure reporter is doing work'() {
        when:
        project.plugins.apply(JavaPlugin)
        project.plugins.apply(InfoBrokerPlugin)
        project.plugins.apply(InfoJavaPlugin)
        def infoPropertiesFilePlugin = project.plugins.apply(InfoPropertiesFilePlugin)
        project.evaluate() // For Java plugin compatibility fields
        InfoPropertiesFile manifestTask = infoPropertiesFilePlugin.getManifestTask()

        then:
        manifestTask.getPropertiesFile() == new File(projectDir, 'build/manifest/ensure-reporter-is-doing-work.properties')
        // Gradle would have done this for us.
        manifestTask.getPropertiesFile().parentFile.mkdirs()

        when:
        manifestTask.writeOut()

        then:
        def result = new Properties()
        def file = manifestTask.getPropertiesFile()
        result.load(new FileInputStream(file))
        result.containsKey InfoJavaPlugin.JDK_PROPERTY
        result.containsKey InfoJavaPlugin.TARGET_PROPERTY

        // patterns like 1.7.0_25
        result[InfoJavaPlugin.JDK_PROPERTY] =~ /\d+\.\d+\.\d_\d{1,3}/

        // one or more digits followed by one or more digits expecting strings like 1.6 or 1.7
        result[InfoJavaPlugin.TARGET_PROPERTY] =~ /\d+\.\d+/
    }

}
