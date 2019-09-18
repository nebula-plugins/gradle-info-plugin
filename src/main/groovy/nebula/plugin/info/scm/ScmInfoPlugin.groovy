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

package nebula.plugin.info.scm

import groovy.transform.CompileDynamic
import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoCollectorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.IConventionAware
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class ScmInfoPlugin implements Plugin<Project>, InfoCollectorPlugin {
    private static Logger logger = Logging.getLogger(ScmInfoPlugin)

    protected Project project
    List<ScmInfoProvider> providers
    ScmInfoProvider selectedProvider
    ScmInfoExtension extension

    @Override
    void apply(Project project) {
        this.project = project

        // TODO Delay findProvider() as long as possible
        providers = [new GitScmProvider(), new PerforceScmProvider(), new SvnScmProvider(), new UnknownScmProvider()] as List<ScmInfoProvider>
        selectedProvider = findProvider()

        extension = project.extensions.create('scminfo', ScmInfoExtension)

        configureExtMapping()

        project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin manifestPlugin ->
            manifestPlugin.add('Module-Source') { extension.source }
            manifestPlugin.add('Module-Origin') { extension.origin }
            manifestPlugin.add('Change') { extension.change }
            manifestPlugin.add('Branch') { extension.branch }
        }
    }

    @CompileDynamic
    private void configureExtMapping() {
        ConventionMapping extMapping = ((IConventionAware) extension).getConventionMapping()
        extMapping.origin = { selectedProvider.calculateOrigin(project) }
        extMapping.source = { selectedProvider.calculateSource(project)?.replace(File.separatorChar, '/' as char) }
        extMapping.change = { selectedProvider.calculateChange(project) }
        extMapping.branch = { selectedProvider.calculateBranch(project) }

    }

    ScmInfoProvider findProvider() {
        ScmInfoProvider provider = providers.find { it.supports(project) }
        if (provider) {
            return provider
        } else {
            throw new IllegalStateException('Unable to find a SCM provider, even the Unknown provider')
        }
    }
}