package nebula.plugin.info.reporting

import nebula.plugin.info.InfoBrokerPlugin
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Simply writes out brokers values to a properties file.
 */
class InfoPropertiesFile extends ConventionTask {

    @Input
    Map<String, ?> getManifest() {
        InfoBrokerPlugin manifestPlugin = project.plugins.getPlugin(InfoBrokerPlugin)

        def entireMap = manifestPlugin.buildNonChangingManifest()

        return entireMap
    }

    @OutputFile
    File propertiesFile

    @TaskAction
    def writeOut() {
        InfoBrokerPlugin basePlugin = project.plugins.getPlugin(InfoBrokerPlugin)

        // Gather all values, in contrast to buildNonChangingManifest
        def attrs = basePlugin.buildManifest()

        def manifestStr = attrs.collect { "${it.key}=${it.value}"}.join('\n')
        getPropertiesFile().text = manifestStr
    }
}
