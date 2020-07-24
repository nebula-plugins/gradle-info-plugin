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
import nebula.plugin.info.InfoBrokerPluginExtension
import nebula.plugin.info.InfoReporterPlugin
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.normalization.MetaInfNormalization

/**
 * Inject manifest values. Does not participate in incremental builds as its own task, it proved too difficult
 * as a separate task.
 */
class InfoJarManifestPlugin implements Plugin<Project>, InfoReporterPlugin {

    void apply(Project project) {

        project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin manifestPlugin ->
            // Searching the Gradle code base shows that Archive Tasks are the primary consumers of project.version
            project.tasks.withType(Jar).configureEach { Jar jarTask ->
                jarTask.doFirst {
                    Map<String,String> attrs = manifestPlugin.buildManifest()
                    jarTask.manifest.attributes.putAll(attrs)
                }
            }

            project.afterEvaluate {
                if(GradleKt.versionGreaterThan(project.gradle, "6.6-rc-1")) {
                    configureMetaInfNormalization(project)
                }
            }

        }
    }

    @CompileDynamic
    private void configureMetaInfNormalization(Project project) {
        InfoBrokerPluginExtension extension = project.extensions.getByName('infoBroker') as InfoBrokerPluginExtension
        project.normalization.runtimeClasspath.metaInf(new Action<MetaInfNormalization>() {
            @Override
            void execute(MetaInfNormalization metaInfNormalization) {
                if(extension.ignoreManifestForNormalization.isPresent() && extension.ignoreManifestForNormalization.get()) {
                    metaInfNormalization.ignoreManifest()
                }
                if(extension.ignoreNormalizationCompletely.isPresent() && extension.ignoreNormalizationCompletely.get()) {
                    metaInfNormalization.ignoreCompletely()
                }
                if(extension.ignoredPropertiesForNormalization.isPresent()) {
                    extension.ignoredPropertiesForNormalization.get().each { manifestProperty ->
                        metaInfNormalization.ignoreProperty(manifestProperty)
                    }
                }
                if(extension.ignoredManifestAttributesForNormalization.isPresent()) {
                    extension.ignoredManifestAttributesForNormalization.get().each { manifestAttribute ->
                        metaInfNormalization.ignoreAttribute(manifestAttribute)
                    }
                }
            }
        })
    }
}
