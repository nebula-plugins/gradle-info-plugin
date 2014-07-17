package nebula.plugin.info.ci

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoCollectorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.IConventionAware

class ContinuousIntegrationInfoPlugin implements Plugin<Project>, InfoCollectorPlugin {
    protected Project project
    List<ContinuousIntegrationInfoProvider> providers
    ContinuousIntegrationInfoProvider selectedProvider
    ContinuousIntegrationInfoExtension extension

    @Override
    void apply(Project project) {
        this.project = project

        providers = [new JenkinsProvider(), new UnknownContinuousIntegrationProvider()]
        selectedProvider = findProvider()

        extension = project.extensions.create('ciinfo', ContinuousIntegrationInfoExtension)

        def extMapping = ((IConventionAware) extension).getConventionMapping()
        extMapping.host = { selectedProvider.calculateHost(project) }
        extMapping.job = { selectedProvider.calculateJob(project) }
        extMapping.buildNumber = { selectedProvider.calculateBuildNumber(project) }
        extMapping.buildId = { selectedProvider.calculateBuildId(project) }

        project.plugins.withType(InfoBrokerPlugin) { manifestPlugin ->
            manifestPlugin.add('Build-Host') { extension.host }
            manifestPlugin.add('Build-Job') { extension.job }
            manifestPlugin.add('Build-Number') { extension.buildNumber }
            manifestPlugin.add('Build-Id') { extension.buildId }
        }

    }

    ContinuousIntegrationInfoProvider findProvider() {
        def provider = providers.find { it.supports(project) }
        if (provider) {
            return provider
        } else {
            throw new IllegalStateException('Unable to find a SCM provider, even the Unknown provider')
        }
    }
}