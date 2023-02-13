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
package nebula.plugin.info.dependencies

import groovy.transform.CompileDynamic
import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoCollectorPlugin
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.VersionInfo
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.DefaultVersionComparator
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParser

class DependenciesInfoPlugin implements Plugin<Project>, InfoCollectorPlugin {
    private final DefaultVersionComparator versionComparator = new DefaultVersionComparator()
    private final VersionParser versionParser = new VersionParser()
    private static final List<String> RESOLVABLE_WITH_DEPRECATION_CONF_SUFFIXES = ['compileOnly', 'compile', 'runtime']

    @Override
    void apply(Project project) {
        setInfoDependencies(project)
        def dependencyMap = project.rootProject.property('nebulaInfoDependencies')
        Map dependencies = [:]
        project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin manifestPlugin ->
            processProjectDependencies(project, dependencies)
            dependencyMap["${project.name}-dependencies".toString()] = dependencies
            if (project == project.rootProject) {
                manifestPlugin.addReport('resolved-dependencies', dependencyMap)
            }
        }
    }

    private void processProjectDependencies(Project project, Map dependencies) {
        project.configurations.all({ Configuration conf ->
            if (canBeResolved(conf)) {
                conf.incoming.afterResolve { ResolvableDependencies resolvableDependencies ->
                    if (project.configurations.contains(conf)) {
                        processIncomingDependencies(resolvableDependencies, 'Resolved-Dependencies', dependencies)
                    }
                }
            }
        })
    }
    private void processIncomingDependencies(ResolvableDependencies resolvableDependencies, String prefix, Map dependencies) {
        String resolvedDependencies = resolvableDependencies.resolutionResult.allComponents.findAll {
            it.id instanceof ModuleComponentIdentifier
        }*.moduleVersion
                .sort(true, { m1, m2 ->
                    if (m1.group != m2.group) {
                        return m1.group <=> m2.group ?: -1
                    }
                    if (m1.name != m2.name) {
                        return m1.name <=> m2.name
                    } // name is required
                    versionComparator.compare(new VersionInfo(versionParser.transform(m1.version)), new VersionInfo(versionParser.transform(m2.version)))
                })*.toString().join(',')
        if (resolvedDependencies) {
            dependencies.put("$prefix-${resolvableDependencies.name.capitalize()}", resolvedDependencies)
        }
    }

    private boolean canBeResolved(Configuration conf) {
        conf.isCanBeResolved() && !RESOLVABLE_WITH_DEPRECATION_CONF_SUFFIXES.any { conf.name.toLowerCase().endsWith(it.toLowerCase()) }
    }

    @CompileDynamic
    private void setInfoDependencies(Project project) {
        if (!project.rootProject.hasProperty('nebulaInfoDependencies')) {
            project.rootProject.ext.nebulaInfoDependencies = [:]
        }
    }
}
