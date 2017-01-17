/*
 * Copyright 2016 Netflix, Inc.
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
package nebula.plugin.info.dependency

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.dependencies.DependenciesInfoPlugin
import nebula.test.PluginProjectSpec
import org.gradle.api.artifacts.ResolveException
import spock.lang.Unroll

class DependenciesInfoPluginSpec extends PluginProjectSpec {
    def 'omits manifest entry if no dependencies'() {
        setup:
        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        project.apply plugin: DependenciesInfoPlugin
        project.configurations.compile.resolve()

        when:
        def manifest = brokerPlugin.buildManifest()

        then:
        noExceptionThrown()
        !manifest.containsKey('Resolved-Dependencies-Compile')
    }

    @Override
    String getPluginName() {
        'nebula.info-dependencies'
    }
}
