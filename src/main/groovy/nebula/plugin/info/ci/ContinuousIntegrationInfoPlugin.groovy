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

package nebula.plugin.info.ci

import groovy.transform.CompileDynamic
import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoCollectorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.IConventionAware

class ContinuousIntegrationInfoPlugin implements Plugin<Project>, InfoCollectorPlugin {
    protected Project project
    List<ContinuousIntegrationInfoProvider> providers
    ContinuousIntegrationInfoProvider selectedProvider
    ContinuousIntegrationInfoExtension extension

    @Override
    void apply(Project project) {
        this.project = project

        providers = [new DroneProvider(), new GitlabProvider(), new JenkinsProvider(), new TravisProvider(), new UnknownContinuousIntegrationProvider()] as List<ContinuousIntegrationInfoProvider>
        selectedProvider = findProvider()

        extension = project.extensions.create('ciinfo', ContinuousIntegrationInfoExtension)

        configureExtMapping()

        project.plugins.withType(InfoBrokerPlugin) {  InfoBrokerPlugin manifestPlugin ->
            manifestPlugin.add('Build-Host') { extension.host }
            manifestPlugin.add('Build-Job') { extension.job }
            manifestPlugin.add('Build-Number') { extension.buildNumber }
            manifestPlugin.add('Build-Id') { extension.buildId }
        }

    }

    @CompileDynamic
    private void configureExtMapping() {
        ConventionMapping extMapping = ((IConventionAware) extension).getConventionMapping()
        extMapping.host = { selectedProvider.calculateHost(project) }
        extMapping.job = { selectedProvider.calculateJob(project) }
        extMapping.buildNumber = { selectedProvider.calculateBuildNumber(project) }
        extMapping.buildId = { selectedProvider.calculateBuildId(project) }
    }

    ContinuousIntegrationInfoProvider findProvider() {
        ContinuousIntegrationInfoProvider provider = providers.find { it.supports(project) }
        if (provider) {
            return provider
        } else {
            throw new IllegalStateException('Unable to find a SCM provider, even the Unknown provider')
        }
    }
}