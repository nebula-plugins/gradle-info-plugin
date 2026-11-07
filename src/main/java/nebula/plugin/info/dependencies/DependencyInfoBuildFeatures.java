package nebula.plugin.info.dependencies;

import org.gradle.api.configuration.BuildFeatures;
import javax.inject.Inject;

/**
 * Adapts Gradle 8.5+ build features for the legacy dependency-report guard.
 *
 * <p>Kept in a separate class so older supported Gradle versions do not load the
 * unavailable {@link BuildFeatures} API. The collector uses the start parameter
 * directly on those versions.
 */
public abstract class DependencyInfoBuildFeatures {
    /**
     * Returns the build feature service supplied by Gradle.
     *
     * @return injected feature state for this build
     */
    @Inject protected abstract BuildFeatures getBuildFeatures();

    /**
     * Reports whether configuration cache is active, including activation by Isolated Projects.
     *
     * <p>Despite the method name, this reads the feature's active state rather than
     * only an explicit command-line request.
     *
     * @return whether legacy resolution-callback reports must be rejected
     */
    public boolean isConfigurationCacheRequested() {
        return getBuildFeatures().getConfigurationCache().getActive().get();
    }
}
