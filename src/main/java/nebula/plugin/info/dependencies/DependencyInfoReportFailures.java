package nebula.plugin.info.dependencies;

import org.gradle.api.artifacts.component.ProjectComponentSelector;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.UnresolvedDependencyResult;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildServiceRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Validates failures from the root task's lenient project-report artifact view.
 *
 * <p>Projects without the collector and broker do not publish a report variant and
 * may be skipped. A missing or broken variant on a registered participant must fail
 * aggregation. Other resolution failures must also fail, even for non-participants,
 * so lenient artifact selection cannot silently turn a broken report into partial data.
 */
public final class DependencyInfoReportFailures implements Callable<List<String>> {
    private final Provider<ResolvedComponentResult> root;
    private final BuildServiceRegistry services;

    /**
     * Creates deferred validation for the aggregate report's project dependencies.
     *
     * @param root lazy root component of the report-collection configuration
     * @param services registry used to distinguish participants from unrelated projects
     */
    public DependencyInfoReportFailures(Provider<ResolvedComponentResult> root, BuildServiceRegistry services) {
        this.root = root;
        this.services = services;
    }

    /**
     * Evaluates the report graph and describes failures that aggregation must reject.
     *
     * @return dependency display names and cause messages, or an empty list when acceptable
     */
    @Override
    public List<String> call() {
        List<String> failures = new ArrayList<>();
        root.get().getDependencies().forEach(dependency -> {
            if (dependency instanceof UnresolvedDependencyResult) {
                UnresolvedDependencyResult unresolved = (UnresolvedDependencyResult) dependency;
                String details = details(unresolved.getFailure());
                boolean participant = !(unresolved.getAttempted() instanceof ProjectComponentSelector)
                        || services.getRegistrations().findByName(DependencyInfoReports.SERVICE_PREFIX
                        + ((ProjectComponentSelector) unresolved.getAttempted()).getProjectPath()) != null;
                boolean missingVariant = details.contains("configuration 'nebulaDependencyInfoElements'")
                        && (details.contains("no variant with that configuration name exists")
                        || details.contains("which is not declared in the descriptor for project"));
                if (participant || !missingVariant) {
                    failures.add(unresolved.getAttempted().getDisplayName() + ": " + details);
                }
            }
        });
        return failures;
    }

    private static String details(Throwable failure) {
        StringBuilder result = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (result.length() > 0) result.append("; ");
            result.append(current.getMessage());
        }
        return result.toString();
    }
}
