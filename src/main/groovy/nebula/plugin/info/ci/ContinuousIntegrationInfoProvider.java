package nebula.plugin.info.ci;

import org.gradle.api.Project;

/**
 * Could contribute to idea plugin, once we know the right SCM
 */
public interface ContinuousIntegrationInfoProvider {
    /**
     * Determine support. Attempt to not use a library, to reduce impact and side effect of calling
     * @param project
     */
    boolean supports(Project project);
    String calculateHost(Project project);
    String calculateJob(Project project);
    String calculateBuildNumber(Project project);
    String calculateBuildId(Project project);
}
