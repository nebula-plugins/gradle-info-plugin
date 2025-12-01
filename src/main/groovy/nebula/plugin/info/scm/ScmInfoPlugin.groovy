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

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoCollectorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory

import javax.inject.Inject

class ScmInfoPlugin implements Plugin<Project>, InfoCollectorPlugin {
    static final String MODULE_SOURCE_PROPERTY = 'Module-Source'
    static final String MODULE_ORIGIN_PROPERTY = 'Module-Origin'
    static final String CHANGE_PROPERTY = 'Change'
    static final String FULL_CHANGE_PROPERTY = 'Full-Change'
    static final String BRANCH_PROPERTY = 'Branch'

    List<ScmInfoProvider> providers
    ScmInfoProvider selectedProvider

    private final ProviderFactory providerFactory

    @Inject
    ScmInfoPlugin(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory
    }

    @Override
    void apply(Project project) {
        configureProviders(project)
        if(project.rootProject == project) {
            configureWithScmProvider(project)
        } else if(project.rootProject != project) {
            handleSubProject(project)
        }
    }

    /**
     * Creates a list of providers for support SCMs and selects based on current repository
     * If a subproject applies the plugin and root project is already applied, we re-use the provider detection
     * @param project
     */
    private void configureProviders(Project project) {
        providers = [new GitScmProvider(providerFactory), new PerforceScmProvider(providerFactory), new UnknownScmProvider(providerFactory)] as List<ScmInfoProvider>
        selectedProvider = findProvider(project)
    }

    /**
     * Configures a subproject scm provider
     * We re-use existing information if the root project already applied to avoid calculating SCM information for each project
     * in a multi-module setup
     * @param project
     */
    private void handleSubProject(Project project) {
        ScmInfoExtension scmInfoRootProjectExtension = project.rootProject.extensions.findByType(ScmInfoExtension)
        if(!scmInfoRootProjectExtension) {
            configureWithScmProvider(project)
        } else {
            configureWithoutScmProvider(project, scmInfoRootProjectExtension)
        }
    }

    /**
     * Configures ScmInfoExtension using selected provider
     * @param project
     */
    private void configureWithScmProvider(Project project) {
        ScmInfoExtension extension = project.extensions.create('scminfo', ScmInfoExtension)
        project.logger.debug("Project $project.name SCM information is being collected from provider ${selectedProvider.class.name}")
        configureExtensionFromProvider(project, extension)
        configureInfoBrokerManifest(project, extension)
    }

    /**
     * Configures ScmInfoExtension using existing rootProject configuration
     * @param project
     */
    private void configureWithoutScmProvider(Project project, ScmInfoExtension scmInfoRootProjectExtension) {
        ScmInfoExtension extension = project.extensions.create('scminfo', ScmInfoExtension)
        project.logger.debug("Project $project.name SCM information is being collected from rootProject extension")
        configureExtensionFromRootProject(extension, scmInfoRootProjectExtension)
        configureInfoBrokerManifest(project, extension)
    }

    private void configureExtensionFromProvider(Project project, ScmInfoExtension extension) {
        extension.origin.convention(providerFactory.provider { selectedProvider.calculateOrigin(project) })
        extension.source.convention(providerFactory.provider { selectedProvider.calculateSource(project)?.replace(File.separatorChar, '/' as char) })
        extension.change.convention(providerFactory.provider { selectedProvider.calculateChange(project) })
        extension.fullChange.convention(providerFactory.provider { selectedProvider.calculateFullChange(project) })
        extension.branch.convention(providerFactory.provider { selectedProvider.calculateBranch(project) })
    }

    private void configureExtensionFromRootProject(ScmInfoExtension extension, ScmInfoExtension scmInfoRootProjectExtension) {
        extension.origin.convention(scmInfoRootProjectExtension.origin)
        extension.source.convention(scmInfoRootProjectExtension.source)
        extension.change.convention(scmInfoRootProjectExtension.change)
        extension.fullChange.convention(scmInfoRootProjectExtension.fullChange)
        extension.branch.convention(scmInfoRootProjectExtension.branch)
    }

    /**
     * Adds scm information to info broker manifest to be used for publications
     * @param project
     * @param scmInfoExtension
     */
    private void configureInfoBrokerManifest(Project project, ScmInfoExtension scmInfoExtension ) {
        project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin manifestPlugin ->
            manifestPlugin.add(MODULE_SOURCE_PROPERTY) { scmInfoExtension.source.getOrNull() }
            manifestPlugin.add(MODULE_ORIGIN_PROPERTY) { scmInfoExtension.origin.getOrNull() }
            manifestPlugin.add(CHANGE_PROPERTY) { scmInfoExtension.change.getOrNull() }
            manifestPlugin.add(FULL_CHANGE_PROPERTY) { scmInfoExtension.fullChange.getOrNull() }
            manifestPlugin.add(BRANCH_PROPERTY) { scmInfoExtension.branch.getOrNull() }
        }
    }

    /**
     * Returns a SCM provider based on project setup
     * If we find an existing config in the root project, we re-use the provider to avoid calculating this multiple times
     * @param project
     * @return
     */
    ScmInfoProvider findProvider(Project project) {
        ScmInfoExtension scmInfoRootProjectExtension = project.rootProject.extensions.findByType(ScmInfoExtension)
        if(scmInfoRootProjectExtension) {
            return project.rootProject.plugins.findPlugin(ScmInfoPlugin).selectedProvider
        }

        ScmInfoProvider provider = providers.find { it.supports(project) }
        if (provider) {
            return provider
        } else {
            throw new IllegalStateException('Unable to find a SCM provider, even the Unknown provider')
        }
    }
}
