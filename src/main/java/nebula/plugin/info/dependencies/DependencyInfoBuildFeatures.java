package nebula.plugin.info.dependencies;

import org.gradle.api.configuration.BuildFeatures;
import javax.inject.Inject;

/** Kept separate so older Gradle versions do not need to load BuildFeatures. */
public abstract class DependencyInfoBuildFeatures {
    @Inject protected abstract BuildFeatures getBuildFeatures();

    public boolean isConfigurationCacheRequested() {
        return getBuildFeatures().getConfigurationCache().getActive().get();
    }
}
