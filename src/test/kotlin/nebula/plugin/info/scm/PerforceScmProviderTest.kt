package nebula.plugin.info.scm

import nebula.plugin.info.getKey
import nebula.plugin.info.readJarAttributes
import nebula.test.dsl.*
import nebula.test.dsl.TestKitAssertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class PerforceScmProviderTest {

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
    }

    @Test
    fun `test perforce file`() {
        val runner = testProject(projectDir) {
            exampleProject()
            rootProject {
                plugins {
                    id("java-library")
                    id("com.netflix.nebula.info-broker")
                    id("com.netflix.nebula.info-scm")
                    id("com.netflix.nebula.info-jar")
                }
            }
        }
        projectDir.resolve("p4config").apply {
            createNewFile()
            writeText(
                """
P4CLIENT=jryan_uber
P4USER=jryan
"""
            )
        }
        val result = runner.run("jar", "-Pversion=1.0", "--stacktrace") {
            withEnvironment(
                mapOf(
                    "P4CONFIG" to "p4config",
                    "P4_CHANGELIST" to "change"
                )
            )
            forwardOutput()
        }

        assertThat(result)
            .hasNoDeprecationWarnings()
            .hasNoMutableStateWarnings()

        val attributes = readJarAttributes(projectDir, "module", "1.0")
        assertThat(attributes.getKey("Module-Source")).isEqualTo(".")
        assertThat(attributes.getKey("Module-Origin")).isEqualTo("p4java://perforce:1666?userName=jryan")
        assertThat(attributes.getKey("Change")).isEqualTo("change")
        assertThat(attributes.getKey("Full-Change")).isEqualTo("change")
        assertThat(attributes.getKey("Branch")).isNull()
    }

    @Test
    fun `test perforce file from subproject`() {
        val runner = testProject(projectDir) {
            exampleProject()
            subProject("sub") {
                plugins {
                    id("java-library")
                    id("com.netflix.nebula.info-broker")
                    id("com.netflix.nebula.info-scm")
                    id("com.netflix.nebula.info-jar")
                }
            }
        }
        projectDir.resolve("p4config").apply {
            createNewFile()
            writeText(
                """
P4CLIENT=jryan_uber
P4USER=jryan
"""
            )
        }
        val result = runner.run("jar", "-Pversion=1.0", "--stacktrace") {
            withEnvironment(
                mapOf(
                    "P4CONFIG" to "p4config",
                    "P4_CHANGELIST" to "change"
                )
            )
            forwardOutput()
        }

        assertThat(result)
            .hasNoDeprecationWarnings()
            .hasNoMutableStateWarnings()

        val attributes = readJarAttributes(projectDir.resolve("sub"), "sub", "1.0")
        assertThat(attributes.getKey("Module-Source")).isEqualTo(".")
        assertThat(attributes.getKey("Module-Origin")).isEqualTo("p4java://perforce:1666?userName=jryan")
        assertThat(attributes.getKey("Change")).isEqualTo("change")
        assertThat(attributes.getKey("Full-Change")).isEqualTo("change")
        assertThat(attributes.getKey("Branch")).isNull()
    }

    @Test
    fun `test perforce env`() {
        val runner = testProject(projectDir) {
            exampleProject()
            rootProject {
                plugins {
                    id("java-library")
                    id("com.netflix.nebula.info-broker")
                    id("com.netflix.nebula.info-scm")
                    id("com.netflix.nebula.info-jar")
                }
            }
        }

        val result = runner.run("jar", "-Pversion=1.0", "--stacktrace") {
            withEnvironment(
                mapOf(
                    "P4CLIENT" to "client",
                    "P4_CHANGELIST" to "change",
                    "P4USER" to "jryan",
                    "P4PORT" to "perforce:8888",
                    "WORKSPACE" to this@PerforceScmProviderTest.projectDir.resolve("workspace")
                        .apply { mkdirs() }
                        .absolutePath
                )
            )
        }

        assertThat(result)
            .hasNoDeprecationWarnings()
            .hasNoMutableStateWarnings()

        val attributes = readJarAttributes(projectDir, "module", "1.0")
        assertThat(attributes.getKey("Module-Source")).isEqualTo("//depot/workspace/")
        assertThat(attributes.getKey("Module-Origin")).isEqualTo("p4java://perforce:8888?userName=jryan")
        assertThat(attributes.getKey("Change")).isEqualTo("change")
        assertThat(attributes.getKey("Full-Change")).isEqualTo("change")
        assertThat(attributes.getKey("Branch")).isNull()
    }

    @Test
    fun `test perforce file custom source`() {
        val runner = testProject(projectDir) {
            exampleProject()
            rootProject {
                plugins {
                    id("java-library")
                    id("com.netflix.nebula.info-broker")
                    id("com.netflix.nebula.info-scm")
                    id("com.netflix.nebula.info-jar")
                }
            }
        }
        projectDir.resolve("p4config").apply {
            createNewFile()
            writeText(
                """
P4CLIENT=jryan_uber
P4USER=jryan
"""
            )
        }
        val result = runner.run("jar", "-Pversion=1.0", "--stacktrace") {
            withEnvironment(
                mapOf(
                    "P4CONFIG" to "p4config",
                    "WORKSPACE" to this@PerforceScmProviderTest.projectDir.resolve("workspace")
                        .apply { mkdirs() }
                        .absolutePath
                )
            )
        }

        assertThat(result)
            .hasNoDeprecationWarnings()
            .hasNoMutableStateWarnings()

        val attributes = readJarAttributes(projectDir, "module", "1.0")
        assertThat(attributes.getKey("Module-Source")).isEqualTo("//depot/workspace/")
        assertThat(attributes.getKey("Module-Origin")).isEqualTo("p4java://perforce:1666?userName=jryan")
        assertThat(attributes.getKey("Change")).isNull()
        assertThat(attributes.getKey("Full-Change")).isNull()
        assertThat(attributes.getKey("Branch")).isNull()
    }
}
