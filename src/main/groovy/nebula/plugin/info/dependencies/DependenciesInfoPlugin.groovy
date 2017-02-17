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

class DependenciesInfoPlugin implements Plugin<Project>, InfoCollectorPlugin {

    @Override
    void apply(Project project) {
        def dependencies = [:]
        project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin manifestPlugin ->
            project.configurations.all { Configuration conf ->
                conf.incoming.afterResolve {
                    dependencies
                        .put(it.name, it.resolutionResult.allDependencies)
                }
            }

            if (project == project.rootProject) {
                manifestPlugin.addReport('dependencies', ["${project.name}": dependencies])
            }
        }
    }
}
