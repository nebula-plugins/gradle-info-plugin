package nebula.plugin.info

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

class InfoBrokerPluginExtension {
    private final ObjectFactory objectFactory

    InfoBrokerPluginExtension(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory
    }

    ListProperty<String> includedManifestProperties = objectFactory.listProperty(String).convention([])
    ListProperty<String> excludedManifestProperties = objectFactory.listProperty(String).convention([])

    /**
     * Ignore all files and subdirectories in the {@code META-INF} directory within archives.
     */
    Property<Boolean> ignoreManifestForNormalization = objectFactory.property(Boolean)

    /**
     * Ignore the {@code META-INF/MANIFEST.MF} file within archives.
     */
    Property<Boolean> ignoreNormalizationCompletely = objectFactory.property(Boolean)

    /**
     * Ignore keys in properties files stored in {@code META-INF} within archives matching {@code name}. {@code name} is matched case-sensitively with the property key.
     */
    ListProperty<String> ignoredPropertiesForNormalization = objectFactory.listProperty(String)

    /**
     * Ignore attributes in {@code META-INF/MANIFEST.MF} within archives matching {@code name}. {@code name} is matched case-insensitively with the manifest attribute name.
     */
    ListProperty<String> ignoredManifestAttributesForNormalization = objectFactory.listProperty(String)
}
