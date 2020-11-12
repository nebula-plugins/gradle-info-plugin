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

package nebula.plugin.info

import nebula.plugin.info.basic.BasicInfoPlugin
import nebula.plugin.info.basic.ManifestOwnersPlugin
import nebula.plugin.info.ci.ContinuousIntegrationInfoPlugin
import nebula.plugin.info.dependencies.DependenciesInfoPlugin
import nebula.plugin.info.java.InfoJavaPlugin
import nebula.plugin.info.reporting.InfoJarManifestPlugin
import nebula.plugin.info.reporting.InfoJarPropertiesFilePlugin
import nebula.plugin.info.reporting.InfoPropertiesFilePlugin
import nebula.plugin.info.scm.ScmInfoPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Pull together all the plugins
 */
class InfoPlugin implements Plugin<Project> {

    public static final List<String> NORMALIZATION_IGNORED_PROPERTY_NAMES = [
            'Implementation-Title',
            'Implementation-Version',
            'Module-Owner',
            'Module-Email',
            'Nebula-Version',
            BasicInfoPlugin.BUILD_STATUS_PROPERTY,
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

        // Broker
        project.plugins.apply(InfoBrokerPlugin)

        // Collectors
        project.plugins.apply(BasicInfoPlugin)
        project.plugins.apply(ManifestOwnersPlugin)
        project.plugins.apply(ScmInfoPlugin)
        project.plugins.apply(ContinuousIntegrationInfoPlugin)
        project.plugins.apply(InfoJavaPlugin)

        // Reporting
        project.plugins.apply(InfoPropertiesFilePlugin)
        project.plugins.apply(InfoJarPropertiesFilePlugin)
        project.plugins.apply(InfoJarManifestPlugin)

    }
}
