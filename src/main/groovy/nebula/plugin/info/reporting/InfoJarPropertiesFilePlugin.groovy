package nebula.plugin.info.reporting

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoReporterPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/**
 * Inject a properties file into the jar file will the info values, using the InfoPropertiesFilePlugin
 */
class InfoJarPropertiesFilePlugin implements Plugin<Project>, InfoReporterPlugin {

    void apply(Project project) {

        project.plugins.withType(InfoBrokerPlugin) { manifestPlugin ->
            def propFilePlugin = project.plugins.apply(InfoPropertiesFilePlugin)
            def manifestTask = propFilePlugin.getManifestTask()

            project.tasks.withType(Jar) { Jar jarTask ->
                jarTask.from(manifestTask.outputs) {
                    into "META-INF"
                }
            }
        }
    }
}