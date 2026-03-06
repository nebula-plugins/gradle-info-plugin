package nebula.plugin.info.scm;

import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;

/**
 * Utility class for executing read-only Git operations using Gradle's Provider API.
 * This class provides a safe, caching-friendly way to access Git repository information
 * during build execution without modifying the repository state.
 *
 * <p>The utility supports configuration cache by using Gradle's Provider system
 * for executing Git commands. All Git operations are delegated to specialized
 * command classes that implement proper parameter passing and caching behavior.</p>
 */
public class GitReadOnlyCommandUtil implements Serializable {
    private final Provider<String> currentBranchProvider;
    private final Provider<String> fullChangeProvider;
    private final Provider<String> remoteOriginProvider;
    private final Provider<String> moduleSourceProvider;

    public static GitReadOnlyCommandUtil create(File rootDir, ProviderFactory providerFactory) {
        return new GitReadOnlyCommandUtil(rootDir, providerFactory);
    }

    GitReadOnlyCommandUtil(File rootDir, ProviderFactory providerFactory) {
        this.currentBranchProvider = providerFactory.of(CurrentBranchGitCommand.class, it ->
                it.parameters(params -> params.getRootDir().set(rootDir)));
        this.remoteOriginProvider = providerFactory.of(GetRemoteOriginGitCommand.class, it ->
                it.parameters(params -> params.getRootDir().set(rootDir)));
        this.fullChangeProvider = providerFactory.of(FullChangeGitCommand.class, it ->
                it.parameters(params -> params.getRootDir().set(rootDir)));
        this.moduleSourceProvider = providerFactory.of(ModuleSourceGitCommand.class, it ->
                it.parameters(params -> params.getRootDir().set(rootDir)));
    }

    /**
     * Get the current branch of the git repository
     */
    public Provider<String> currentBranch() {
        return currentBranchProvider.map(it -> it.replaceAll("\n", "").trim());
    }

    public Provider<String> moduleSource() {
        return moduleSourceProvider.map(it -> it.replaceAll("\n", "").trim());
    }

    public Provider<String> fullChange() {
        return fullChangeProvider.map(it -> it.replaceAll("\n", "").trim());
    }

    /**
     * Get the remote origin of the git repository
     */
    public Provider<String> remoteOrigin() {
        return remoteOriginProvider.map(it -> it.replaceAll("\n", "").trim());
    }

}
