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
import org.gradle.api.provider.ProviderFactory

import javax.inject.Inject

class ScmInfoPlugin implements Plugin<Project>, InfoCollectorPlugin {
    public static final String MODULE_SOURCE_PROPERTY = 'Module-Source'
    public static final String MODULE_ORIGIN_PROPERTY = 'Module-Origin'
    public static final String CHANGE_PROPERTY = 'Change'
    public static final String FULL_CHANGE_PROPERTY = 'Full-Change'
    public static final String BRANCH_PROPERTY = 'Branch'

    List<ScmInfoProvider> providers
    ScmInfoProvider selectedProvider

    private final ProviderFactory providerFactory

    @Inject
    ScmInfoPlugin(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory
    }

    @Override
    void apply(Project project) {
        // TODO Delay findProvider() as long as possible
        providers = [new GitScmProvider(providerFactory), new PerforceScmProvider(providerFactory), new SvnScmProvider(providerFactory), new UnknownScmProvider(providerFactory)] as List<ScmInfoProvider>
        selectedProvider = findProvider(project)

        ScmInfoExtension extension = project.extensions.create('scminfo', ScmInfoExtension)

        configureExtMapping(project, extension)

        project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin manifestPlugin ->
            manifestPlugin.add(MODULE_SOURCE_PROPERTY) { extension.source }
            manifestPlugin.add(MODULE_ORIGIN_PROPERTY) { extension.origin }
            manifestPlugin.add(CHANGE_PROPERTY) { extension.change }
            manifestPlugin.add(FULL_CHANGE_PROPERTY) { extension.fullChange }
            manifestPlugin.add(BRANCH_PROPERTY) { extension.branch }
        }
    }

    @CompileDynamic
    private void configureExtMapping(Project project, ScmInfoExtension extension) {
        ConventionMapping extMapping = ((IConventionAware) extension).getConventionMapping()
        extMapping.origin = { selectedProvider.calculateOrigin(project) }
        extMapping.source = { selectedProvider.calculateSource(project)?.replace(File.separatorChar, '/' as char) }
        extMapping.change = { selectedProvider.calculateChange(project) }
        extMapping.fullChange = { selectedProvider.calculateFullChange(project) }
        extMapping.branch = { selectedProvider.calculateBranch(project) }
    }

    ScmInfoProvider findProvider(Project project) {
        ScmInfoProvider provider = providers.find { it.supports(project) }
        if (provider) {
            return provider
        } else {
            throw new IllegalStateException('Unable to find a SCM provider, even the Unknown provider')
        }
    }
}
