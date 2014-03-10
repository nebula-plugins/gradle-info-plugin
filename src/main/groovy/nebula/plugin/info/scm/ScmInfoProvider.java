package nebula.plugin.info.scm;

import org.gradle.api.Project;

/**
 * Could contribute to idea plugin, once we know the right SCM
 */
public interface ScmInfoProvider {
    /**
     * Determine support. Attempt to not use a library, to reduce impact and side effect of calling
     * @param project
     * @return
     */
    boolean supports(Project project);
    String calculateSource(Project project);
    String calculateOrigin(Project project);
    String calculateChange(Project project);
}
