package nebula.plugin.info.scm;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IsolatedScmInfoTest {
    private static final String FLAG = "nebula.info.isolatedProjectsSupport";
    private static final String ORIGIN = "https://example.org/shared.git";
    @TempDir Path directory;

    @ParameterizedTest
    @CsvSource({"9.0.0,true", "9.0.0,false", "9.2.1,true", "9.2.1,false"})
    void sharedGitRepositoryWorksWithRootOrChildOnlyApplication(String version, boolean applyRoot) throws Exception {
        fixture(applyRoot);
        repository(project(), ORIGIN);
        checkTwice(version, ":api:writeScm", ":platform:app:writeScm");
        assertScm("api", ORIGIN, "/api");
        assertScm("platform/app", ORIGIN, "/platform/app");
    }

    @Test
    void localOverridesDoNotLeakBetweenProjects() throws Exception {
        fixture(true);
        repository(project(), ORIGIN);
        append("build.gradle", "scminfo.origin.set('https://example.org/root-only.git')\n");
        append("api/build.gradle", "scminfo.origin.set('https://example.org/api-only.git')\n");
        checkTwice("9.2.1", ":writeScm", ":api:writeScm", ":platform:app:writeScm");
        assertScm("", "https://example.org/root-only.git", "");
        assertScm("api", "https://example.org/api-only.git", "/api");
        assertScm("platform/app", ORIGIN, "/platform/app");
    }

    @Test
    void nestedGitRepositoriesSelectTheirOwnOrigin() throws Exception {
        fixture(true);
        repository(project(), ORIGIN);
        repository(project().resolve("platform/app"), "https://example.org/nested.git");
        checkTwice("9.2.1", ":api:writeScm", ":platform:app:writeScm");
        assertScm("api", ORIGIN, "/api");
        assertScm("platform/app", "https://example.org/nested.git", "");
    }

    @Test
    void worktreeGitFileIsSupported() throws Exception {
        Path seed = directory.resolve("seed");
        repository(seed, ORIGIN);
        git(seed, "worktree", "add", "--detach", project().toString());
        fixture(true);
        checkTwice("9.2.1", ":api:writeScm", ":platform:app:writeScm");
        assertScm("api", ORIGIN, "/api");
    }

    @ParameterizedTest
    @ValueSource(strings = {"9.0.0", "9.2.1"})
    void noGitFallbackDoesNotCaptureAProject(String version) throws Exception {
        fixture(true);
        checkTwice(version, ":writeScm", ":api:writeScm", ":platform:app:writeScm");
        assertScm("api", "LOCAL", project().resolve("api").toRealPath().toString());
        assertScm("platform/app", "LOCAL", project().resolve("platform/app").toRealPath().toString());
    }

    @Test
    void changingRemoteRefreshesCachedMetadata() throws Exception {
        fixture(true);
        repository(project(), ORIGIN);
        checkTwice("9.2.1", ":api:writeScm");
        git(project(), "remote", "set-url", "origin", "https://example.org/changed.git");
        run("9.2.1", true, Map.of(), ":api:writeScm");
        assertScm("api", "https://example.org/changed.git", "/api");
    }

    @Test
    void customProjectDirectoriesUseLogicalAncestry() throws Exception {
        fixture(true);
        repository(project(), ORIGIN);
        Path external = directory.resolve("external-module");
        Files.createDirectories(external);
        Files.writeString(external.resolve("build.gradle"), script());
        append("settings.gradle", "project(':api').projectDir = file('" + external + "')\n");
        checkTwice("9.2.1", ":api:writeScm");
        assertEquals(ORIGIN, Files.readAllLines(external.resolve("build/scm.txt")).get(0));
    }

    @Test
    void perforceConfigProvidersSurviveCacheReuseAndRefresh() throws Exception {
        fixture(true);
        write(".p4config", "P4PORT=perforce.invalid:1666\nP4USER=fixture\n");
        Map<String, String> environment = Map.of("P4CONFIG", ".p4config", "P4CLIENT", "fixture",
                "WORKSPACE", project().toString(), "P4_CHANGELIST", "123");
        run("9.2.1", true, environment, ":api:writeScm");
        assertTrue(run("9.2.1", true, environment, ":api:writeScm").getOutput().contains("Reusing configuration cache."));
        assertEquals("p4java://perforce.invalid:1666?userName=fixture", Files.readAllLines(project().resolve("api/build/scm.txt")).get(0));
        write(".p4config", "P4PORT=changed.invalid:1666\nP4USER=fixture\n");
        run("9.2.1", true, environment, ":api:writeScm");
        assertEquals("p4java://changed.invalid:1666?userName=fixture", Files.readAllLines(project().resolve("api/build/scm.txt")).get(0));
    }

    @Test
    void flagOffRetainsRootMetadataInheritance() throws Exception {
        fixture(true);
        repository(project(), ORIGIN);
        write("gradle.properties", FLAG + "=false\n");
        append("build.gradle", "scminfo.origin.set('https://example.org/root-override.git')\n");
        run("9.2.1", false, Map.of(), ":api:writeScm");
        assertScm("api", "https://example.org/root-override.git", "");
    }

    private void checkTwice(String version, String... tasks) {
        assertTrue(run(version, true, Map.of(), tasks).getOutput().contains("Configuration cache entry stored."));
        assertTrue(run(version, true, Map.of(), tasks).getOutput().contains("Reusing configuration cache."));
    }

    private BuildResult run(String version, boolean isolated, Map<String, String> overrides, String... tasks) {
        List<String> args = new ArrayList<>(List.of(tasks));
        args.addAll(List.of("--configuration-cache", "--configuration-cache-problems=fail", "--parallel", "--max-workers=2",
                "-Dorg.gradle.unsafe.isolated-projects=" + isolated,
                "-Dorg.gradle.java.home=" + System.getProperty("scm.testJavaHome"), "--stacktrace"));
        Map<String, String> environment = new HashMap<>(System.getenv());
        for (String name : List.of("GIT_COMMIT", "P4CONFIG", "P4CLIENT", "WORKSPACE", "P4_CHANGELIST")) environment.remove(name);
        environment.putAll(overrides);
        return GradleRunner.create().withProjectDir(project().toFile()).withGradleVersion(version)
                .withPluginClasspath().withEnvironment(environment).withArguments(args).build();
    }

    private void fixture(boolean applyRoot) throws IOException {
        write("settings.gradle", "rootProject.name = 'scm-fixture'\ninclude 'api', 'platform:app'\n");
        write("gradle.properties", FLAG + "=true\n");
        write("build.gradle", applyRoot ? script() : "");
        write("api/build.gradle", script());
        write("platform/build.gradle", "");
        write("platform/app/build.gradle", script());
    }

    private String script() {
        return """
            plugins { id 'com.netflix.nebula.info-scm' }
            abstract class WriteScm extends DefaultTask {
                @Input abstract Property<String> getOrigin()
                @Input abstract Property<String> getSource()
                @Input abstract Property<String> getRevision()
                @OutputFile abstract RegularFileProperty getDestinationFile()
                @TaskAction void writeMetadata() {
                    destinationFile.get().asFile.text = origin.get() + '\\n' + source.get() + '\\n' + revision.get()
                }
            }
            tasks.register('writeScm', WriteScm) {
                origin.set(scminfo.origin)
                source.set(scminfo.source)
                revision.set(scminfo.fullChange.orElse('NONE'))
                destinationFile.set(layout.buildDirectory.file('scm.txt'))
            }
            """;
    }

    private void assertScm(String projectPath, String origin, String source) throws IOException {
        List<String> values = Files.readAllLines(project().resolve(projectPath).resolve("build/scm.txt"));
        assertEquals(origin, values.get(0));
        assertEquals(source, values.get(1));
    }

    private Path project() { return directory.resolve("project"); }

    private void write(String path, String contents) throws IOException {
        Path target = project().resolve(path);
        Files.createDirectories(target.getParent());
        Files.writeString(target, contents);
    }

    private void append(String path, String contents) throws IOException {
        Files.writeString(project().resolve(path), Files.readString(project().resolve(path)) + contents);
    }

    private void repository(Path path, String origin) throws Exception {
        Files.createDirectories(path);
        git(path, "init", "-b", "main");
        git(path, "-c", "user.name=Fixture", "-c", "user.email=fixture@example.org", "commit", "--allow-empty", "-m", "Fixture");
        git(path, "remote", "add", "origin", origin);
    }

    private void git(Path path, String... arguments) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", path.toString()));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertEquals(0, process.waitFor(), output);
    }
}
