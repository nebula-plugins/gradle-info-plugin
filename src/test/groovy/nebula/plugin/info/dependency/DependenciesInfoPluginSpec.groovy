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
import org.gradle.api.internal.artifacts.configurations.ConflictResolution

class DependenciesInfoPluginSpec extends PluginProjectSpec {
    def 'reports requestedDependencies if zero-sized'() {
        setup:
        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        project.apply plugin: DependenciesInfoPlugin
        project.configurations.compile.resolve()


        when:
        brokerPlugin.buildFinished.set(true)
        def reports = brokerPlugin.buildReports()

        then:
        noExceptionThrown()
        reports.containsKey('requestedDependencies')
    }

    def 'reports resolvedDependencies if zero-sized'() {
        setup:
        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        project.apply plugin: DependenciesInfoPlugin
        project.configurations.compile.resolve()


        when:
        brokerPlugin.buildFinished.set(true)
        def reports = brokerPlugin.buildReports()

        then:
        noExceptionThrown()
        reports.containsKey('resolvedDependencies')
    }

    def 'reports excludes if zero-sized'() {
        setup:
        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        project.apply plugin: DependenciesInfoPlugin
        project.configurations.compile.resolve()


        when:
        brokerPlugin.buildFinished.set(true)
        def reports = brokerPlugin.buildReports()

        then:
        noExceptionThrown()
        reports.containsKey('excludes')
    }

    def 'reports resolutionStrategies if zero-sized'() {
        setup:
        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        project.apply plugin: DependenciesInfoPlugin
        project.configurations.compile.resolve()


        when:
        brokerPlugin.buildFinished.set(true)
        def reports = brokerPlugin.buildReports()

        then:
        noExceptionThrown()
        reports.containsKey('resolutionStrategies')
    }

    def 'only includes configurations on configuration container'() {
        setup:
        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        project.apply plugin: DependenciesInfoPlugin

        def guava = project.dependencies.create('com.google.guava:guava:21.0')

        project.repositories.add(project.repositories.mavenCentral())

        def configurations = project.configurations
        def config = configurations.compile
        config.dependencies.add(guava)

        def detached = configurations.detachedConfiguration(guava)
        configurations.add(detached)
        configurations.remove(detached)

        config.resolve()
        detached.resolve()

        when:
        brokerPlugin.buildFinished.set(true)
        def reports = brokerPlugin.buildReports()

        then:
        reports['resolvedDependencies']['only-includes-configurations-on-configuration-container'].size() == 1
    }

    def 'reports requestedDependencies'() {
        setup:
        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        project.apply plugin: DependenciesInfoPlugin

        def spark = project.dependencies.create('org.apache.spark:spark-parent:1.2.2')

        project.repositories.add(project.repositories.mavenCentral())

        def configurations = project.configurations

        def compile = configurations.compile
        compile.dependencies.add(spark)

        compile.resolve()

        when:
        brokerPlugin.buildFinished.set(true)
        def reports = brokerPlugin.buildReports()

        then:
        noExceptionThrown()
        reports['requestedDependencies']['reports-requestedDependencies']['compile'].size() == 1
    }

    def 'reports resolvedDependencies'() {
        setup:
        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        project.apply plugin: DependenciesInfoPlugin

        def spark = project.dependencies.create('org.apache.spark:spark-parent:1.2.2')

        project.repositories.add(project.repositories.mavenCentral())

        def configurations = project.configurations

        def compile = configurations.compile
        compile.dependencies.add(spark)

        compile.resolve()

        when:
        brokerPlugin.buildFinished.set(true)
        def reports = brokerPlugin.buildReports()

        then:
        noExceptionThrown()
        reports['resolvedDependencies']['reports-resolvedDependencies']['compile']
                .findAll { it.moduleVersion.id.toString() == 'org.apache.spark:spark-parent' }
                .size() == 1

    }

    def 'reports the dependency model per Gradle without transforming it'() {
        setup:
        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        project.apply plugin: DependenciesInfoPlugin

        def spark = project.dependencies.create('org.apache.spark:spark-parent:1.2.2', {
            exclude group: 'org.apache.spark', module: 'unused'
        })

        project.repositories.add(project.repositories.mavenCentral())

        def configurations = project.configurations

        def compile = configurations.compile
        compile.dependencies.add(spark)

        compile.resolve()

        when:
        brokerPlugin.buildFinished.set(true)
        def reports = brokerPlugin.buildReports()

        then:
        noExceptionThrown()
        // the excludes are attached via Gradle's own internal model
        reports['requestedDependencies']['reports-the-dependency-model-per-Gradle-without-transforming-it']['compile'][0]
                .getExcludeRules()
                .size() == 1
    }

    def 'reports excludes'() {
        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        project.apply plugin: DependenciesInfoPlugin

        project.repositories.add(project.repositories.mavenCentral())

        def configurations = project.configurations

        def compile = configurations.compile
        compile.exclude(["group": "org.apache.spark", "module": "spark-parent"])
        compile.resolve()

        when:
        brokerPlugin.buildFinished.set(true)
        def reports = brokerPlugin.buildReports()

        then:
        noExceptionThrown()
        // the excludes are attached via Gradle's own internal model
        reports['excludes']['reports-excludes']['compile'].size() == 1
    }

    def 'reports resolution strategies'() {
        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        project.apply plugin: DependenciesInfoPlugin

        project.repositories.add(project.repositories.mavenCentral())

        def configurations = project.configurations

        def compile = configurations.compile
        compile.resolutionStrategy {
            failOnVersionConflict()
        }
        compile.resolve()

        when:
        brokerPlugin.buildFinished.set(true)
        def reports = brokerPlugin.buildReports()

        then:
        noExceptionThrown()
        reports['resolutionStrategies']['reports-resolution-strategies']['compile']
                .conflictResolution == ConflictResolution.strict

    }

    @Override
    String getPluginName() {
        'nebula.info-dependencies'
    }
}
