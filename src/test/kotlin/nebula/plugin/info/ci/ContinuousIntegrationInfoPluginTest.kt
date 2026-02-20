package nebula.plugin.info.ci

import nebula.test.dsl.*
import nebula.test.dsl.TestKitAssertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.Attributes
import java.util.jar.JarFile

internal class ContinuousIntegrationInfoPluginTest {

    @TempDir
    lateinit var projectDir: File

    private fun TestProjectBuilder.exampleProject() {
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
                id("com.netflix.nebula.info-ci")
                id("com.netflix.nebula.info-jar")
            }
        }
    }

    @Test
    fun `test Github Actions`() {
        val runner = testProject(projectDir) {
            exampleProject()
        }
        val result = runner.run("jar", "-Pversion=1.0") {
            withEnvironment(
                mapOf(
                    "CI" to "true",
                    "GITHUB_ACTION" to "my-action",
                    "GITHUB_REPOSITORY" to "org/my-repo",
                    "GITHUB_RUN_NUMBER" to "10",
                    "GITHUB_RUN_ID" to "1",
                    "GITHUB_SERVER_URL" to "http://some-github"
                )
            )
        }

        assertThat(result)
            .hasNoDeprecationWarnings()
            .hasNoMutableStateWarnings()

        val attributes = readJarAttributes()
        assertThat(attributes.getKey("Build-Job")).isEqualTo("my-action")
        assertThat(attributes.getKey("Build-Number")).isEqualTo("10")
        assertThat(attributes.getKey("Build-Id")).isEqualTo("1")
        assertThat(attributes.getKey("Build-Url")).isEqualTo("http://some-github/org/my-repo/actions/runs/1")
        assertThat(attributes.getKey("Build-Host")).isEqualTo("http://some-github")
    }

    @Test
    fun `test Jenkins`() {
        val runner = testProject(projectDir) {
            exampleProject()
        }
        val result = runner.run("jar", "-Pversion=1.0") {
            withEnvironment(
                mapOf(
                    "JOB_NAME" to "org/my-repo",
                    "Build-Job" to "org/my-repo",
                    "BUILD_NUMBER" to "10",
                    "BUILD_ID" to "1",
                    "BUILD_URL" to "http://leeroy-jenkins/2",
                    "JENKINS_URL" to "http://leeroy-jenkins",
                )
            )
        }

        assertThat(result)
            .hasNoDeprecationWarnings()
            .hasNoMutableStateWarnings()

        val attributes = readJarAttributes()
        assertThat(attributes.getKey("Build-Job")).isEqualTo("org/my-repo")
        assertThat(attributes.getKey("Build-Number")).isEqualTo("10")
        assertThat(attributes.getKey("Build-Id")).isEqualTo("1")
        assertThat(attributes.getKey("Build-Url")).isEqualTo("http://leeroy-jenkins/2")
        assertThat(attributes.getKey("Build-Host")).isEqualTo("http://leeroy-jenkins")
    }

    fun Attributes.getKey(key: String): Any? {
        return get(Attributes.Name(key))
    }

    fun readJarAttributes(): Attributes {
        return JarFile(projectDir.resolve("build/libs/module-1.0.jar")).manifest.mainAttributes
    }
}