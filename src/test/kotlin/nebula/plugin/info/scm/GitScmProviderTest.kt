package nebula.plugin.info.scm

import nebula.plugin.info.getKey
import nebula.plugin.info.readJarAttributes
import nebula.plugin.info.testutil.TestHelpers.withRemoteGit
import nebula.test.dsl.*
import nebula.test.dsl.TestKitAssertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class GitScmProviderTest {
    @TempDir
    lateinit var projectDir: File

    @TempDir
    lateinit var remoteGitDir: File

    @Test
    fun `test git with remote`() {
        withRemoteGit(remoteGitDir, projectDir) { remote, working ->
            val runner = testProject(projectDir) {
                properties {
                    buildCache(true)
                    configurationCache(true)
                }
                settings {
                    name("module")
                }
                rootProject {
                    plugins {
                        id("java-library")
                        id("com.netflix.nebula.info-broker")
                        id("com.netflix.nebula.info-scm")
                        id("com.netflix.nebula.info-jar")
                    }
                }
            }
            val result = runner.run("jar", "-Pversion=1.0", "--stacktrace")

            assertThat(result)
                .hasNoDeprecationWarnings()
                .hasNoMutableStateWarnings()

            val attributes = readJarAttributes(projectDir, "module", "1.0")
            assertThat(attributes.getKey("Module-Origin")).isEqualTo("file:" + remoteGitDir.absolutePath + "/.git")
        }
    }
}
