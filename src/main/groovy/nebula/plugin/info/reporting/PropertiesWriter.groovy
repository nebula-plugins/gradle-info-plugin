package nebula.plugin.info.reporting

import nebula.plugin.info.InfoBrokerPlugin
import org.gradle.api.Project

class PropertiesWriter {
    private PropertiesWriter() {
    }

    static void writeProperties(File location, InfoBrokerPlugin basePlugin) {
        // Gather all values, in contrast to buildNonChangingManifest
        Map<String, String> attrs = basePlugin.buildManifest()

        String manifestStr = attrs.collect { "${it.key}=${it.value}"}.join('\n')
        location.text = manifestStr
    }
}
