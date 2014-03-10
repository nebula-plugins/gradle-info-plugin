package nebula.plugin.info

import nebula.plugin.info.basic.BasicInfoPlugin
import nebula.plugin.info.ci.ContinuousIntegrationInfoPlugin
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

    void apply(Project project) {

        // Broker
        project.plugins.apply(InfoBrokerPlugin)

        // Collectors
        project.plugins.apply(BasicInfoPlugin)
        project.plugins.apply(ScmInfoPlugin)
        project.plugins.apply(ContinuousIntegrationInfoPlugin)
        project.plugins.apply(InfoJavaPlugin)

        // Reporting
        project.plugins.apply(InfoPropertiesFilePlugin)
        project.plugins.apply(InfoJarPropertiesFilePlugin)
        project.plugins.apply(InfoJarManifestPlugin)

    }
}
