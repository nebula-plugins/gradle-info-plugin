package nebula.plugin.info.scm

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoCollectorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
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
        providers = [new GitScmProvider(), new PerforceScmProvider(), new UnknownScmProvider()]
        selectedProvider = findProvider()

        extension = project.extensions.create('scminfo', ScmInfoExtension)

        def extMapping = ((IConventionAware) extension).getConventionMapping()
        extMapping.origin = { selectedProvider.calculateOrigin(project) }
        extMapping.source = { selectedProvider.calculateSource(project) }
        extMapping.change = { selectedProvider.calculateChange(project) }

        project.plugins.withType(InfoBrokerPlugin) { manifestPlugin ->
            manifestPlugin.add('Module-Source') { extension.source }
            manifestPlugin.add('Module-Origin') { extension.origin }
            manifestPlugin.add('Change') { extension.change }
        }

    }

    ScmInfoProvider findProvider() {
        def provider = providers.find { it.supports(project) }
        if (provider) {
            return provider
        } else {
            throw new IllegalStateException('Unable to find a SCM provider, even the Unknown provider')
        }
    }
}