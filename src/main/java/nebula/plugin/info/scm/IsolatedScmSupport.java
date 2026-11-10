package nebula.plugin.info.scm;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.util.GradleVersion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration-only SCM discovery through immutable project directory identities.
 * Project references never leave these methods; deferred providers retain only directories.
 */
final class IsolatedScmSupport {
    private static final String FLAG = "nebula.info.isolatedProjectsSupport";

    private IsolatedScmSupport() {}

    static boolean enabled(ProviderFactory providers) {
        boolean enabled = providers.gradleProperty(FLAG).map(Boolean::parseBoolean).getOrElse(false);
        if (enabled && GradleVersion.current().compareTo(GradleVersion.version("8.8")) < 0) {
            throw new GradleException(FLAG + " requires Gradle 8.8 or newer.");
        }
        return enabled;
    }

    /** Preserves logical project ancestry, including projects with custom directories. */
    static List<Directory> projectDirectories(Project project) {
        List<Directory> directories = new ArrayList<>();
        for (Project current = project; current != null; current = current.getParent()) {
            directories.add(current.getIsolated().getProjectDirectory());
        }
        return List.copyOf(directories);
    }

    static RegularFile findFile(List<Directory> directories, String name) {
        for (Directory directory : directories) {
            RegularFile file = directory.file(name);
            if (file.getAsFile().exists()) return file;
        }
        return null;
    }

    /** The provider captures immutable directories rather than a live Project. */
    static Provider<RegularFile> perforceConfig(ProviderFactory providers, List<Directory> directories) {
        return providers.environmentVariable("P4CONFIG").map(name -> findFile(directories, name));
    }

    static File rootDirectory(Project project) {
        return project.getIsolated().getRootProject().getProjectDirectory().getAsFile();
    }
}
