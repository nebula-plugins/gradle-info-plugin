package nebula.plugin.info

import groovy.transform.CompileStatic
import org.eclipse.jgit.transport.URIish
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static nebula.plugin.info.testutil.TestHelpers.withGit
import static nebula.test.dsl.GroovyTestProjectBuilder.testProject
import static org.assertj.core.api.Assertions.assertThat

@CompileStatic
class InfoBrokerPluginTest {

    @TempDir
    File projectDir

    String buildLogicForTesting = """
group = 'test.nebula'

def broker = project.plugins.getPlugin(${InfoBrokerPlugin.name})
afterEvaluate {
    println "manifest: " + broker.buildManifest()
}
"""

    @Test
    void integration_test() {
        withGit(projectDir, (working) -> {
            def remoteAdd = working.remoteAdd()
            remoteAdd.name = "origin"
            remoteAdd.uri = new URIish("https://something.com/repo.git")
            remoteAdd.call()
            def runner = testProject(projectDir) {
                properties {
                    configurationCache(true)
                    buildCache(true)
                }
                rootProject {
                    plugins {
                        id("com.netflix.nebula.info-scm")
                        id("com.netflix.nebula.info-broker")
                    }
                    rawBuildScript(buildLogicForTesting)
                }
            }

            def result = runner.run("build")
            assertThat(result.output)
                    .as("the origin url should be correct")
                    .contains("Module-Origin:https://something.com/repo.git,")
            assertThat(result.output)
                    .as("when building and Git is present, we should get a full-change attribute")
                    .contains("Full-Change:")
        })
    }

    @Test
    void test_origin_url_normalization() {
        withGit(projectDir, (working) -> {
            def remoteAdd = working.remoteAdd()
            remoteAdd.name = "origin"
            remoteAdd.uri = new URIish("https://something.com/repo")
            remoteAdd.call()
            def runner = testProject(projectDir) {
                properties {
                    configurationCache(true)
                    buildCache(true)
                }
                rootProject {
                    plugins {
                        id("com.netflix.nebula.info-scm")
                        id("com.netflix.nebula.info-broker")
                    }
                    rawBuildScript(buildLogicForTesting)
                }
            }

            def result = runner.run("build")
            assertThat(result.output)
                    .as("the origin url should be normalized")
                    .contains("Module-Origin:https://something.com/repo.git,")
        })
    }
}
