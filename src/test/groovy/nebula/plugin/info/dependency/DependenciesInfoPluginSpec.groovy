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
import nebula.test.dependencies.DependencyGraphBuilder
import nebula.test.dependencies.GradleDependencyGenerator
import nebula.test.dependencies.ModuleBuilder

class DependenciesInfoPluginSpec extends PluginProjectSpec {
    def 'adds a dependencies entry to the reports even if there are no dependencies'() {
        setup:
        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        project.apply plugin: DependenciesInfoPlugin
        project.configurations.compile.resolve()

        when:
        def reports = brokerPlugin.buildReports()

        then:
        noExceptionThrown()
        reports.containsKey("dependencies")
    }


    @Override
    String getPluginName() {
        'nebula.info-dependencies'
    }

    def 'adds per-configuration dependency information to the reports'() {
        setup:
        def repoPath = "build/testrepogen"

        new GradleDependencyGenerator(
            new DependencyGraphBuilder()
                .addModule(
                    new ModuleBuilder("test.example:foo:1.0.0")
                        .addDependency("test.example:bar:1.0.0")
                        .build()

                )
                .addModule(
                    new ModuleBuilder("test.example:baz:1.0.0")
                        .addDependency("test.example:bar:2.0.0")
                        .build()
                )
                .build(),
            repoPath
        )
            .generateTestMavenRepo()


        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        project.apply plugin: DependenciesInfoPlugin
        project.dependencies {
            compile "test.example:foo:1.0.0"
            testCompile "test.example:baz:1.0.0"
        }
        project.repositories {
            maven {
                url {
                    new File("$repoPath/mavenrepo")
                        .absolutePath
                }
            }
        }
        project.configurations.compile.resolve()
        project.configurations.testCompile.resolve()

        when:
        def dependencyReportForProject = brokerPlugin
            .buildReports()
            .get("dependencies")
            .entrySet()[0]
            .value as Map<String, Object>

        then:
        noExceptionThrown()
        dependencyReportForProject.containsKey("compile") && dependencyReportForProject.containsKey("testCompile")
    }


    def 'does not add dependency information to the manifest'() {
        setup:
        def repoPath = "build/testrepogen"

        new GradleDependencyGenerator(
            new DependencyGraphBuilder()
                .addModule(
                    new ModuleBuilder("test.example:foo:1.0.0")
                        .addDependency("test.example:bar:1.0.0")
                        .build()

                )
                .build(),
            repoPath
        )
            .generateTestMavenRepo()


        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        project.apply plugin: DependenciesInfoPlugin
        project.dependencies {
            compile "test.example:foo:1.0.0"
        }
        project.repositories {
            maven {
                url {
                    new File("$repoPath/mavenrepo")
                        .absolutePath
                }
            }
        }
        project.configurations.compile.resolve()
        project.configurations.testCompile.resolve()

        when:
        def manifest = brokerPlugin.buildManifest()

        then:
        noExceptionThrown()
        !manifest.containsKey("dependencies")
    }
}
