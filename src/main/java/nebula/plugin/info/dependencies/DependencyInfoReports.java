package nebula.plugin.info.dependencies;

import org.gradle.api.provider.Provider;
import org.gradle.api.GradleException;
import org.gradle.api.services.BuildServiceRegistry;
import org.gradle.api.services.BuildServiceRegistration;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;

/** A read-only, lazy compatibility view of the project-owned report inputs. */
public final class DependencyInfoReports extends AbstractMap<String, Map<String, String>> {
    public static final String SERVICE_PREFIX = "nebulaDependencyInfo:";
    private final Provider<List<DependencyInfoState.Parameters>> sources;
    private List<DependencyInfoState.Parameters> legacySources;
    private final Map<String, Map<String, String>> initialEntries;

    public DependencyInfoReports(Provider<List<DependencyInfoState.Parameters>> sources, Map<String, Map<String, String>> initialEntries) {
        this.sources = sources;
        this.initialEntries = initialEntries;
    }

    @Override
    public Set<Entry<String, Map<String, String>>> entrySet() {
        Map<String, Map<String, String>> result = new TreeMap<>();
        result.putAll(initialEntries);
        for (DependencyInfoState.Parameters inputs : legacySources != null ? legacySources : sources.get()) {
            if (inputs.getConfigurationCacheRequested().get()) {
                throw new GradleException("Use the writeResolvedDependencies task output for dependency reports with configuration cache. "
                        + "Declare project-local inputs with dependencyInfo.report(configuration) or dependencyInfo.reportNone(). "
                        + "Resolution callbacks are not replayed when reusing the configuration cache.");
            }
            Map<String, String> dependencies = inputs.getDependencies().get();
            result.put(inputs.getProjectName().get() + "-dependencies", Collections.unmodifiableMap(new TreeMap<>(dependencies)));
        }
        return Collections.unmodifiableMap(result).entrySet();
    }

    public void captureLegacySources() {
        legacySources = sources.get();
    }

    /**
     * ProviderFactory.provider evaluates this lookup when storing the configuration cache.
     * Returned inputs contain properties and values, rather than another project's model.
     */
    public static final class Sources implements Callable<List<DependencyInfoState.Parameters>> {
        private final BuildServiceRegistry registry;
        private final List<String> projectPaths;

        public Sources(BuildServiceRegistry registry, List<String> projectPaths) {
            this.registry = registry;
            this.projectPaths = projectPaths;
        }

        @Override
        public List<DependencyInfoState.Parameters> call() {
            List<DependencyInfoState.Parameters> result = new ArrayList<>();
            for (String path : projectPaths) {
                BuildServiceRegistration<?, ?> registration = registry.getRegistrations().findByName(SERVICE_PREFIX + path);
                if (registration != null) {
                    result.add((DependencyInfoState.Parameters) registration.getParameters());
                }
            }
            return result;
        }
    }
}
