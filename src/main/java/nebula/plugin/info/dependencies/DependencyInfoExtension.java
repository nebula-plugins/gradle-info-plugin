package nebula.plugin.info.dependencies;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.util.Map;

/**
 * Selects the project-local configurations included by {@code writeDependencyInfo}.
 *
 * <p>The {@code dependencyInfo} extension is added by the dependency-info collector.
 * Apply the info broker as well to register the report tasks. Each participating
 * project must select its own inputs, for example:
 * <pre>{@code
 * dependencyInfo.report(configurations.named('runtimeClasspath'))
 * }</pre>
 *
 * <p>Selection does not resolve dependencies. The selected resolution graphs are
 * queried when the report inputs are evaluated, which may happen while Gradle
 * stores the configuration cache. Unselected configurations are not resolved.
 * Only external module components are reported, including transitive modules;
 * project components themselves are omitted. Unresolved selected dependencies
 * fail the report instead of producing a partial result.
 *
 * <p>These explicit inputs are independent of the legacy callback-based map.
 * Consumers using configuration cache must consume the report task's output file.
 */
public class DependencyInfoExtension {
    private final MapProperty<String, String> dependencies;
    private final Property<Boolean> explicitSelection;
    private final Provider<Map<String, String>> empty;

    /**
     * Creates the extension with no selected configurations.
     *
     * @param objects Gradle's factory for managed report input properties
     */
    @Inject
    public DependencyInfoExtension(ObjectFactory objects) {
        dependencies = objects.mapProperty(String.class, String.class).convention(java.util.Collections.emptyMap());
        explicitSelection = objects.property(Boolean.class).convention(false);
        empty = objects.mapProperty(String.class, String.class).convention(java.util.Collections.emptyMap());
    }

    /**
     * Adds a configuration to this project's report without resolving it.
     *
     * <p>Calls accumulate selections. Non-resolvable configurations and names ending
     * in {@code compileOnly}, {@code compile}, or {@code runtime}, ignoring case,
     * are filtered out. A filtered or empty configuration contributes no report key,
     * but the call still counts as an explicit selection.
     *
     * @param configuration a configuration belonging to the project that owns this extension
     */
    public void report(Configuration configuration) {
        explicitSelection.set(true);
        if (DependenciesInfoSupport.canBeReported(configuration)) {
            dependencies.putAll(configuration.getIncoming().getResolutionResult().getRootComponent()
                    .map(new DependenciesInfoSupport.Notation(configuration.getName())));
        }
    }

    /**
     * Adds a lazily supplied configuration, such as {@code configurations.named(...)}.
     *
     * <p>The provider is not queried by this method. When report inputs are evaluated,
     * the same filtering and resolution rules as {@link #report(Configuration)} apply.
     *
     * @param configuration provider of a configuration belonging to this project
     */
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

    /**
     * Explicitly allows a project to produce a report with no selected configurations.
     *
     * <p>This satisfies the report task's selection requirement without resolving any
     * configuration. It does not clear selections made by earlier {@code report} calls.
     */
    public void reportNone() {
        explicitSelection.set(true);
    }

    /**
     * Returns the lazy module notation map wired into {@code writeDependencyInfo}.
     *
     * <p>Keys use {@code Resolved-Dependencies-<CapitalizedConfigurationName>}.
     * Values are comma-separated {@code group:name:version} coordinates sorted by
     * group, module name, and Gradle's version comparator. Prefer the selection
     * methods to populating this task input directly.
     *
     * @return accumulated report entries; querying the value may resolve selected configurations
     */
    public MapProperty<String, String> getDependencies() {
        return dependencies;
    }

    /**
     * Returns whether the build has explicitly selected report inputs, including none.
     *
     * <p>The report task rejects the default {@code false} value. Use {@code report}
     * or {@link #reportNone()} to express the project's intent.
     *
     * @return the selection flag used to validate the report task
     */
    public Property<Boolean> getExplicitSelection() {
        return explicitSelection;
    }
}
