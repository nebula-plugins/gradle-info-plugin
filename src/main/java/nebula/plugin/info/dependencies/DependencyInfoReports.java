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

/**
 * Exposes naturally resolved dependencies through the legacy {@code nebulaInfoDependencies} map.
 *
 * <p>The root collector supplies an aggregate view of participating projects; a child
 * collector supplies only its own project. Keys use {@code <project-name>-dependencies},
 * with configuration entries as described by {@link DependencyInfoExtension#getDependencies()}.
 * Reading this view does not initiate dependency resolution.
 *
 * <p>The outer map and collected project maps are unmodifiable. Each read snapshots
 * the currently observed values, so a previously obtained entry set does not update
 * after further resolutions. Preexisting report entries are retained, but their
 * nested values are not defensively copied. This differs from the former shared,
 * mutable root map and must not be used as a mutable collection of report inputs.
 *
 * <p>Resolution callbacks are not replayed on configuration-cache reuse. Evaluating
 * a participating project's entries while configuration cache is active therefore
 * fails with migration guidance. Use {@code dependencyInfo.report(...)} and the
 * {@code writeDependencyInfo} or root {@code writeResolvedDependencies} output instead.
 */
public final class DependencyInfoReports extends AbstractMap<String, Map<String, String>> {
    /** Prefix for per-project service registrations, followed by the project path. */
    public static final String SERVICE_PREFIX = "nebulaDependencyInfo:";
    private final Provider<List<DependencyInfoState.Parameters>> sources;
    private List<DependencyInfoState.Parameters> legacySources;
    private final Map<String, Map<String, String>> initialEntries;

    /**
     * Creates a view over project observations and previously supplied report entries.
     *
     * @param sources lazy lookup of the participating projects' input properties
     * @param initialEntries existing entries to retain; observed project entries take precedence
     */
    public DependencyInfoReports(Provider<List<DependencyInfoState.Parameters>> sources, Map<String, Map<String, String>> initialEntries) {
        this.sources = sources;
        this.initialEntries = initialEntries;
    }

    /**
     * Snapshots the available legacy report entries without resolving configurations.
     *
     * @return an unmodifiable entry set sorted by project report key
     * @throws GradleException if a participating project's configuration cache is active
     */
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

    /**
     * Captures the participating projects after project evaluation for no-cache builds.
     *
     * <p>Only the source list is captured; its properties continue to reflect later
     * resolution callbacks. This avoids looking up service registrations at build finish.
     */
    public void captureLegacySources() {
        legacySources = sources.get();
    }

    /**
     * Looks up project-local observations without retaining another project's model.
     *
     * <p>Used as a {@code ProviderFactory.provider} callable. Evaluation consults service
     * registrations and returns their parameters without instantiating the services.
     * The provider may be evaluated while storing the configuration cache.
     */
    public static final class Sources implements Callable<List<DependencyInfoState.Parameters>> {
        private final BuildServiceRegistry registry;
        private final List<String> projectPaths;

        /**
         * Creates a lookup restricted to the supplied project paths.
         *
         * @param registry shared service registry for this build
         * @param projectPaths project paths to inspect, in report traversal order
         */
        public Sources(BuildServiceRegistry registry, List<String> projectPaths) {
            this.registry = registry;
            this.projectPaths = projectPaths;
        }

        /**
         * Returns parameters for the registered participants among the requested paths.
         *
         * @return parameters in path-list order, omitting projects without a registration
         */
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
