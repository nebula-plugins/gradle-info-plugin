package nebula.plugin.info.dependencies;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/**
 * Registers one project's legacy dependency observations without sharing its project model.
 *
 * <p>The collector stores values in service parameters under a registration named
 * {@link DependencyInfoReports#SERVICE_PREFIX} followed by the project path. The
 * service instance holds no report data and need not be instantiated to read the
 * parameters. Explicit file reports use {@link DependencyInfoExtension} instead.
 */
public abstract class DependencyInfoState implements BuildService<DependencyInfoState.Parameters> {
    /** Managed input properties used by the legacy report view. */
    public interface Parameters extends BuildServiceParameters {
        /**
         * Returns the project name used to form {@code <project-name>-dependencies}.
         *
         * @return project name, rather than its full project path
         */
        Property<String> getProjectName();
        /**
         * Returns module entries collected by this project's resolution callbacks.
         *
         * @return observed configuration entries; this property does not trigger resolution
         */
        MapProperty<String, String> getDependencies();
        /**
         * Returns the configuration-cache flag used to reject unsupported legacy reads.
         *
         * @return whether the collector detected configuration cache as active/requested
         */
        Property<Boolean> getConfigurationCacheRequested();
    }
}
