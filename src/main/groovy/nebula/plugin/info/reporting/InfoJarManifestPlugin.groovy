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
import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoReporterPlugin
import nebula.plugin.info.basic.BasicInfoPlugin
import nebula.plugin.info.ci.ContinuousIntegrationInfoPlugin
import nebula.plugin.info.java.InfoJavaPlugin
import nebula.plugin.info.scm.ScmInfoPlugin
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

    private static final List<String> ATTRIBUTES_TO_IGNORE_FOR_META_INF_NORMALIZATION = [
            'Module-Owner',
            'Module-Email',
            'Nebula-Version',
            BasicInfoPlugin.BUILT_BY_PROPERTY,
            BasicInfoPlugin.BUILT_OS_PROPERTY,
            BasicInfoPlugin.BUILD_DATE_PROPERTY,
            BasicInfoPlugin.GRADLE_VERSION_PROPERTY,
            BasicInfoPlugin.BUILD_STATUS_PROPERTY,
            InfoJavaPlugin.CREATED_PROPERTY,
            InfoJavaPlugin.JDK_PROPERTY,
            InfoJavaPlugin.SOURCE_PROPERTY,
            InfoJavaPlugin.TARGET_PROPERTY,
            ContinuousIntegrationInfoPlugin.BUILD_NUMBER_PROPERTY,
            ContinuousIntegrationInfoPlugin.BUILD_HOST_PROPERTY,
            ContinuousIntegrationInfoPlugin.BUILD_JOB_PROPERTY,
            ContinuousIntegrationInfoPlugin.BUILD_ID_PROPERTY,
            ScmInfoPlugin.MODULE_SOURCE_PROPERTY,
            ScmInfoPlugin.MODULE_ORIGIN_PROPERTY,
            ScmInfoPlugin.CHANGE_PROPERTY,
            ScmInfoPlugin.FULL_CHANGE_PROPERTY,
            ScmInfoPlugin.BRANCH_PROPERTY
    ] as List<String>

    void apply(Project project) {

        project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin manifestPlugin ->
            // Searching the Gradle code base shows that Archive Tasks are the primary consumers of project.version
            project.tasks.withType(Jar).configureEach { Jar jarTask ->
                jarTask.doFirst {
                    Map<String,String> attrs = manifestPlugin.buildManifest()
                    jarTask.manifest.attributes.putAll(attrs)
                }
            }
            if(GradleKt.versionGreaterThan(project.gradle, "6.6-rc-1")) {
                configureMetaInfNormalization(project)
            }
        }
    }

    private void configureMetaInfNormalization(Project project) {
        project.normalization.runtimeClasspath.metaInf(new Action<MetaInfNormalization>() {
            @Override
            void execute(MetaInfNormalization metaInfNormalization) {
                ATTRIBUTES_TO_IGNORE_FOR_META_INF_NORMALIZATION.each { attribute ->
                    metaInfNormalization.ignoreAttribute(attribute)
                }
            }
        })
    }
}
