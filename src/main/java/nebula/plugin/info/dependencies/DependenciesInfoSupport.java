package nebula.plugin.info.dependencies;

import org.gradle.api.Transformer;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.result.UnresolvedDependencyResult;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.VersionInfo;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.DefaultVersionComparator;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Shared filtering, module ordering, and graph traversal for both reporting paths. */
final class DependenciesInfoSupport {
    private DependenciesInfoSupport() {}

    /** Preserves the legacy exclusions for non-resolvable and deprecated configuration names. */
    static boolean canBeReported(Configuration configuration) {
        String name = configuration.getName().toLowerCase(java.util.Locale.ROOT);
        return configuration.isCanBeResolved() && !name.endsWith("compileonly")
                && !name.endsWith("compile") && !name.endsWith("runtime");
    }

    /** Formats module coordinates in the legacy group/name/Gradle-version order. */
    static String formatModules(Collection<ModuleVersionIdentifier> values) {
        List<ModuleVersionIdentifier> modules = new ArrayList<>(values);
        DefaultVersionComparator comparator = new DefaultVersionComparator();
        VersionParser parser = new VersionParser();
        modules.sort((left, right) -> {
            int group = left.getGroup().compareTo(right.getGroup());
            if (group != 0) return group;
            int name = left.getName().compareTo(right.getName());
            if (name != 0) return name;
            return comparator.compare(new VersionInfo(parser.transform(left.getVersion())),
                    new VersionInfo(parser.transform(right.getVersion())));
        });
        return modules.stream().map(Object::toString).collect(Collectors.joining(","));
    }

    /**
     * Traverses a selected graph, including transitives, and emits its configuration entry.
     * Retains only the configuration name so provider state does not capture a project
     * or configuration. Unresolved edges fail the report instead of being omitted.
     */
    static final class Notation implements Transformer<Map<String, String>, ResolvedComponentResult> {
        private final String configurationName;

        Notation(String configurationName) {
            this.configurationName = configurationName;
        }

        @Override
        public Map<String, String> transform(ResolvedComponentResult root) {
            List<ModuleVersionIdentifier> modules = new ArrayList<>();
            Set<ComponentIdentifier> visited = new HashSet<>();
            ArrayDeque<ResolvedComponentResult> pending = new ArrayDeque<>();
            pending.add(root);
            while (!pending.isEmpty()) {
                ResolvedComponentResult component = pending.removeFirst();
                if (!visited.add(component.getId())) {
                    continue;
                }
                if (component.getId() instanceof ModuleComponentIdentifier) {
                    modules.add(component.getModuleVersion());
                }
                component.getDependencies().forEach(dependency -> {
                    if (dependency instanceof ResolvedDependencyResult) {
                        pending.add(((ResolvedDependencyResult) dependency).getSelected());
                    } else if (dependency instanceof UnresolvedDependencyResult) {
                        throw new GradleException("Could not resolve dependencies for configuration '" + configurationName + "'",
                                ((UnresolvedDependencyResult) dependency).getFailure());
                    }
                });
            }
            if (modules.isEmpty()) return Collections.emptyMap();
            String key = "Resolved-Dependencies-" + Character.toUpperCase(configurationName.charAt(0)) + configurationName.substring(1);
            return Collections.singletonMap(key, formatModules(modules));
        }
    }
}
