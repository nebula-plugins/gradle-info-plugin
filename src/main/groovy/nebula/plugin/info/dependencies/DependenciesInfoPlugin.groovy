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
package nebula.plugin.info.dependencies

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoCollectorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier

class DependenciesInfoPlugin implements Plugin<Project>, InfoCollectorPlugin {
    @Override
    void apply(Project project) {
        if (!project.rootProject.hasProperty('nebulaInfoDependencies')) {
            project.rootProject.ext.nebulaInfoDependencies = [:]
        }
        if (!project.rootProject.hasProperty('nebulaInfoRequestedDependencies')) {
            project.rootProject.ext.nebulaInfoRequestedDependencies = [:]
        }
        if (!project.rootProject.hasProperty('nebulaInfoExcludes')) {
            project.rootProject.ext.nebulaInfoExcludes = [:]
        }
        if (!project.rootProject.hasProperty('nebulaInfoResolutionStrategies')) {
            project.rootProject.ext.nebulaInfoResolutionStrategies = [:]
        }
        def dependencyMap = project.rootProject.property('nebulaInfoDependencies')
        def dependencies = [:]
        def requestedDependencyMap = project.rootProject.property('nebulaInfoRequestedDependencies')
        def requestedDependencies = [:]
        def excludeMap = project.rootProject.property('nebulaInfoExcludes')
        def excludes = [:]
        def resolutionStrategyMap = project.rootProject.property('nebulaInfoResolutionStrategies')
        def resolutionStrategies = [:]

        project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin manifestPlugin ->
            project.configurations.all( { Configuration conf ->
                conf.incoming.afterResolve { resolvableDependencies ->
                    if (project.configurations.contains(conf)) {
                        def requested = resolvableDependencies.dependencies

                        excludes.put(
                            conf.name,
                            conf.excludeRules
                        )

                        resolutionStrategies.put(
                            conf.name,
                            conf.resolutionStrategy
                        )

                        requestedDependencies
                            .put(conf.name, requested)

                        def resolvedDependencies = resolvableDependencies.resolutionResult.allComponents.findAll {
                            it.id instanceof ModuleComponentIdentifier
                        }
                        if (resolvedDependencies) {
                            dependencies.put(resolvableDependencies.name, resolvedDependencies)
                        }
                    }
                }
            })

            dependencyMap[project.name] = dependencies
            resolutionStrategyMap[project.name] = resolutionStrategies
            excludeMap[project.name] = excludes
            requestedDependencyMap[project.name] = requestedDependencies
            if (project == project.rootProject) {
                manifestPlugin.addReport('resolvedDependencies', dependencyMap)
                manifestPlugin.addReport('requestedDependencies', requestedDependencyMap)
                manifestPlugin.addReport('resolutionStrategies', resolutionStrategyMap)
                manifestPlugin.addReport('excludes', excludeMap)
            }
        }
    }
}
