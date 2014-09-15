package nebula.plugin.info.reporting

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoReporterPlugin
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Jar

/**
 * Inject manifest values. Does not participate in incremental builds as its own task, it proved too difficult
 * as a separate task.
 */
class InfoJarManifestPlugin implements Plugin<Project>, InfoReporterPlugin {

    void apply(Project project) {

        project.plugins.withType(InfoBrokerPlugin) { manifestPlugin ->
            // Searching the Gradle code base shows that Archive Tasks are the primary consumers of project.version
            project.tasks.withType(Jar) { Jar jarTask ->
                project.afterEvaluate {
                    def entireMap = manifestPlugin.buildNonChangingManifest()
                    jarTask.inputs.properties(entireMap)
                }

                jarTask.doFirst {
                    Map<String,String> attrs = manifestPlugin.buildManifest()
                    jarTask.manifest.attributes.putAll(attrs)
                }
            }
        }
    }
}
