package nebula.plugin.info.reporting

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoReporterPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/**
 * Inject a txt file into the jar file will the info values.
 */
class InfoJarManifestPlugin implements Plugin<Project>, InfoReporterPlugin {

    void apply(Project project) {

        project.plugins.withType(InfoBrokerPlugin) { manifestPlugin ->
            // Searching the Gradle code base shows that Archive Tasks are the primary consumers of project.version
            project.tasks.withType(Jar) { Jar jarTask ->
                def manifestTask = project.tasks.create("${jarTask.name}Manifest", ApplyManifest)
                manifestTask.jarTask = jarTask

                // Technically, the jar task depends on the manifest task (even though we need it to modify the manifest)
                jarTask.dependsOn manifestTask

                // Gradle can only expose outputs as Files, so we need to piggyback into the one we're modifying
                manifestTask.jarBeingModified = jarTask.archivePath
            }
        }
    }
}
