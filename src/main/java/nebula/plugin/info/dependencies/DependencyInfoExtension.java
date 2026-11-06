package nebula.plugin.info.dependencies;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.util.Map;

/** Explicit, lazy resolution inputs for reports consumed with configuration cache. */
public class DependencyInfoExtension {
    private final MapProperty<String, String> dependencies;
    private final Property<Boolean> explicitSelection;
    private final Provider<Map<String, String>> empty;

    @Inject
    public DependencyInfoExtension(ObjectFactory objects) {
        dependencies = objects.mapProperty(String.class, String.class).convention(java.util.Collections.emptyMap());
        explicitSelection = objects.property(Boolean.class).convention(false);
        empty = objects.mapProperty(String.class, String.class).convention(java.util.Collections.emptyMap());
    }

    public void report(Configuration configuration) {
        explicitSelection.set(true);
        if (DependenciesInfoSupport.canBeReported(configuration)) {
            dependencies.putAll(configuration.getIncoming().getResolutionResult().getRootComponent()
                    .map(new DependenciesInfoSupport.Notation(configuration.getName())));
        }
    }

    public void report(Provider<? extends Configuration> configuration) {
        explicitSelection.set(true);
        Provider<Map<String, String>> emptyReport = empty;
        dependencies.putAll(configuration.flatMap(value -> {
            if (!DependenciesInfoSupport.canBeReported(value)) {
                return emptyReport;
            }
            return value.getIncoming().getResolutionResult().getRootComponent()
                    .map(new DependenciesInfoSupport.Notation(value.getName()));
        }));
    }

    public void reportNone() {
        explicitSelection.set(true);
    }

    public MapProperty<String, String> getDependencies() {
        return dependencies;
    }

    public Property<Boolean> getExplicitSelection() {
        return explicitSelection;
    }
}
