package nebula.plugin.info.dependencies;

import org.gradle.api.artifacts.component.ProjectComponentSelector;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.UnresolvedDependencyResult;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildServiceRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/** Missing report variants are expected only for projects that do not participate. */
public final class DependencyInfoReportFailures implements Callable<List<String>> {
    private final Provider<ResolvedComponentResult> root;
    private final BuildServiceRegistry services;

    public DependencyInfoReportFailures(Provider<ResolvedComponentResult> root, BuildServiceRegistry services) {
        this.root = root;
        this.services = services;
    }

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
