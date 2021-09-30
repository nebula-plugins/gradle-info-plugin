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

import com.netflix.nebula.interop.GradleKt
import groovy.transform.CompileDynamic
import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoPlugin
import nebula.plugin.info.InfoReporterPlugin
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.normalization.MetaInfNormalization

/**
 * Inject a properties file into the jar file will the info values, using the InfoPropertiesFilePlugin
 */
@CompileDynamic
class InfoJarPropertiesFilePlugin implements Plugin<Project>, InfoReporterPlugin {

    void apply(Project project) {
        project.plugins.withType(JavaBasePlugin) {
            project.plugins.withType(InfoBrokerPlugin) { manifestPlugin ->
                InfoPropertiesFilePlugin propFilePlugin = project.plugins.apply(InfoPropertiesFilePlugin) as InfoPropertiesFilePlugin
                TaskProvider<InfoPropertiesFile> manifestTask = propFilePlugin.getManifestTask()

                File propertiesFile = new File(project.buildDir, "properties_for_jar/${manifestTask.get().propertiesFile.name}")
                TaskProvider<Task> prepareFile = project.tasks.register("createPropertiesFileForJar") { Task task ->
                    task.outputs.file(propertiesFile)
                    task.doLast {
                        //Task action is intentionally creating empty file.
                        propertiesFile.text = ""
                    }
                }

                project.tasks.withType(Jar).configureEach { Jar jarTask ->
                    //explicit dependency on original task to keep contract that `jar` task invocation will produce properties file
                    //even this file is actually not bundled into jar
                    jarTask.dependsOn(manifestTask)

                    //we link the output file from the task to the spec to add it into jar, but the file is empty, it will
                    //help to ignore changes in its content for caching
                    jarTask.metaInf { CopySpec spec ->
                        spec.from(prepareFile)
                    }

                    jarTask.doFirst {
                        //when we are after all caching decisions we fill the file with all the data
                        new PropertiesWriter().writeProperties(propertiesFile, project)
                    }
                    jarTask.doLast {
                        //we need to cleanup file in case we got multiple jar tasks
                        propertiesFile.text = ""
                    }
                }

                if (GradleKt.versionGreaterThan(project.gradle, "6.6-rc-1")) {
                    configureMetaInfNormalization(project)
                }
            }
        }

    }

    private static void configureMetaInfNormalization(Project project) {
        project.normalization.runtimeClasspath.metaInf(new Action<MetaInfNormalization>() {
            @Override
            void execute(MetaInfNormalization metaInfNormalization) {
                InfoPlugin.NORMALIZATION_IGNORED_PROPERTY_NAMES.each { attribute ->
                    metaInfNormalization.ignoreProperty(attribute)
                }
            }
        })
    }
}
