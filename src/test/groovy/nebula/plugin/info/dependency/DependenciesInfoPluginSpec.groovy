/*
 * Copyright 2016-2019 Netflix, Inc.
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
        project.configurations.compileClasspath.resolve()

        when:
        def manifest = brokerPlugin.buildManifest()

        then:
        noExceptionThrown()
        !manifest.containsKey('Resolved-Dependencies-Compile')
    }

    def 'only includes configurations on configuration container'() {
        setup:
        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        project.apply plugin: DependenciesInfoPlugin

        def guava = project.dependencies.create('com.google.guava:guava:21.0')
        def slf4j = project.dependencies.create('org.slf4j:slf4j-api:1.7.30')

        project.repositories.add(project.repositories.mavenCentral())

        def configurations = project.configurations
        def implementationConfig = configurations.implementation
        implementationConfig.dependencies.add(guava)
        implementationConfig.dependencies.add(slf4j)

        def detached = configurations.detachedConfiguration(guava)
        configurations.add(detached)
        configurations.remove(detached)
        
        detached.resolve()
        configurations.compileClasspath.resolve()

        when:
        def reports = brokerPlugin.buildReports()

        then:
        reports['resolved-dependencies']['only-includes-configurations-on-configuration-container-dependencies'].size() == 1
    }

    @Override
    String getPluginName() {
        'nebula.info-dependencies'
    }
}
