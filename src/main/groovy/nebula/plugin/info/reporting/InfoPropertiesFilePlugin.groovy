package nebula.plugin.info.reporting

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoReporterPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.BasePluginConvention

/**
 * Write contents of the manifest to a file, as a property file.
 */
class InfoPropertiesFilePlugin implements Plugin<Project>, InfoReporterPlugin {

    InfoPropertiesFile manifestTask

    void apply(Project project) {

        project.plugins.withType(InfoBrokerPlugin) {  InfoBrokerPlugin basePlugin ->

            manifestTask = project.tasks.create('writeManifestProperties', InfoPropertiesFile)
            manifestTask.conventionMapping.map('propertiesFile') {
                // A little clunky, because there is no way to say, "If there's no convention, run this". E.g.
                // timing is improtant here, this should be running after the BasePlugin is applied if it's going
                // to be applied.
                if (project.plugins.hasPlugin(BasePlugin)) {
                    BasePluginConvention baseConvention = project.getConvention().getPlugin(BasePluginConvention)
                    new File(project.buildDir, "manifest/${baseConvention.archivesBaseName}.properties")
                } else {
                    new File(project.buildDir, "manifest/info.properties")
                }
            }
        }
    }

    InfoPropertiesFile getManifestTask() {
        assert manifestTask != null, 'InfoPropertiesFilePlugin has to be applied first'
        return manifestTask
    }
}
