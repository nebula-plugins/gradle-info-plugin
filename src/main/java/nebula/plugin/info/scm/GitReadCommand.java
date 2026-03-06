package nebula.plugin.info.scm;

import org.gradle.api.GradleException;
import org.gradle.api.provider.ValueSource;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * These read only git commands use ValueSource approach for configuration cache
 *
 * @see <a href="https://docs.gradle.org/8.4/userguide/configuration_cache.html#config_cache:requirements:external_processes">Gradle docs</a>
 */
abstract class GitReadCommand implements ValueSource<String, GitCommandParameters> {
    @Inject
    public abstract ExecOperations getExecOperations();

    /**
     * Execute a git command with the given arguments
     */
    String executeGitCommand(String... args) {
        File rootDir = getParameters().getRootDir().get();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        String gitDir = rootDir.toPath().toAbsolutePath().resolve(".git").toString();
        List<String> commandLineArgs = new ArrayList<>(
                Arrays.asList("git",
                        "--git-dir=" + gitDir,
                        "--work-tree=" + rootDir.getAbsolutePath()));
        commandLineArgs.addAll(Arrays.asList(args));
        getExecOperations().exec(execSpec -> {
            execSpec.setCommandLine(commandLineArgs);
            execSpec.setStandardOutput(output);
            execSpec.setErrorOutput(error);
        });
        String errorMsg = new String(error.toByteArray(), Charset.defaultCharset());
        if (!errorMsg.isEmpty()) {
            throw new GradleException(output + " " + errorMsg);
        }
        return new String(output.toByteArray(), Charset.defaultCharset());
    }
}