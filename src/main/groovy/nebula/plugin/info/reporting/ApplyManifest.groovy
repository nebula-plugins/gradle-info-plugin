package nebula.plugin.info.reporting

import nebula.plugin.info.InfoBrokerPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

/**
 * Use ManifestHelper to create a manifest for the jar.  If we don't capture the input correctly, we could leave the
 * the manifest alone even though some important piece has changed.
 */
class ApplyManifest extends DefaultTask {
    @Input
    String jarTaskName

    @OutputFile
    File jarBeingModified

    Map<String, ?> getManifest() {
        InfoBrokerPlugin manifestPlugin = project.plugins.getPlugin(InfoBrokerPlugin)

        def entireMap = manifestPlugin.buildNonChangingManifest()

        return entireMap
    }

    /**
     * Sets up collected defined attributes into the manifest specification for jar tasks.
     */
    @TaskAction
    void specManifest() {

        InfoBrokerPlugin manifestPlugin = project.plugins.getPlugin(InfoBrokerPlugin)

        Map<String,String> attrs = manifestPlugin.buildManifest()

        Task task = project.tasks.getByName(jarTaskName)

        if(!(task instanceof Jar)) {
            throw new InvalidUserDataException("The task with the provided name '$jarTaskName' is not of type Jar.")
        }

        ((Jar)task).manifest.attributes.putAll(attrs)

    }
}
