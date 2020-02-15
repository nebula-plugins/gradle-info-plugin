package nebula.plugin.info.reporting

import nebula.plugin.info.InfoBrokerPlugin
import org.gradle.api.Project

class PropertiesWriter {

    void writeProperties(File location, Project project) {
        InfoBrokerPlugin basePlugin = project.plugins.getPlugin(InfoBrokerPlugin) as InfoBrokerPlugin

        // Gather all values, in contrast to buildNonChangingManifest
        Map<String, String> attrs = basePlugin.buildManifest()

        String manifestStr = attrs.collect { "${it.key}=${it.value}"}.join('\n')
        location.text = manifestStr
    }
}
