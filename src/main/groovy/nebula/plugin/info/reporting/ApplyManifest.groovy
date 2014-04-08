package nebula.plugin.info.reporting

import nebula.plugin.info.InfoBrokerPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

/**
 * Use ManifestHelper to create a manifest for the jar.  If we don't capture the input correctly, we could leave the
 * the manifest alone even though some important piece has changed.
 *
 * If we mark the jarTask as an @Input, we get a NotSerializableException. If we don't put an @Input on it
 * then the task will only ever run once, even though we need to run every time the jar task is run, since
 * we're configuring it. The logical conclusion is to not participate in the up-to-date check, by not having
 * any Inputs or Outputs.
 */
class ApplyManifest extends DefaultTask {
    Jar jarTask

    Map<String, ?> getManifest() {
        InfoBrokerPlugin manifestPlugin = project.plugins.getPlugin(InfoBrokerPlugin)

        def entireMap = manifestPlugin.buildNonChangingManifest()

        return entireMap
    }

    File jarBeingModified

    /**
     * Sets up collected defined attributes into the manifest specification for jar tasks.
     */
    @TaskAction
    void specManifest() {

        InfoBrokerPlugin manifestPlugin = project.plugins.getPlugin(InfoBrokerPlugin)

        Map<String,String> attrs = manifestPlugin.buildManifest()

        jarTask.manifest.attributes.putAll(attrs)

    }
}
