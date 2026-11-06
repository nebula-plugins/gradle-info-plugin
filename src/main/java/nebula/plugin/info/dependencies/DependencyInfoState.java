package nebula.plugin.info.dependencies;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/** Project-owned report inputs. No report data is stored in the service instance. */
public abstract class DependencyInfoState implements BuildService<DependencyInfoState.Parameters> {
    public interface Parameters extends BuildServiceParameters {
        Property<String> getProjectName();
        MapProperty<String, String> getDependencies();
        Property<Boolean> getConfigurationCacheRequested();
    }
}
