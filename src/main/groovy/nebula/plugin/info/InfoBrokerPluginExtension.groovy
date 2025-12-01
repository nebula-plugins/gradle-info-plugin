package nebula.plugin.info

import org.gradle.api.provider.ListProperty

abstract class InfoBrokerPluginExtension {
    abstract ListProperty<String> getIncludedManifestProperties()
    abstract ListProperty<String> getExcludedManifestProperties()
}
