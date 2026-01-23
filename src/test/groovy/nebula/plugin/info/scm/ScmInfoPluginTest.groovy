package nebula.plugin.info.scm

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static nebula.plugin.info.testutil.TestHelpers.withGit
import static nebula.plugin.info.testutil.TestHelpers.withRemoteGit
import static nebula.test.dsl.GroovyTestProjectBuilder.testProject
import static org.assertj.core.api.Assertions.assertThat

@CompileStatic
class ScmInfoPluginTest {

    @TempDir
    File projectDir

    @TempDir
    File remoteGitDir

    @Test
    void test_origin_with_remote_git() {
        withRemoteGit(remoteGitDir, projectDir, (remote, working) -> {
            def runner = testProject(projectDir) {
                properties {
                    buildCache(true)
                }
                rootProject {
                    plugins {
                        id("com.netflix.nebula.info-scm")
                    }
                    rawBuildScript("""
afterEvaluate {
    println("scminfo.origin = " + scminfo.origin.get())
    println("scminfo.source = " + scminfo.source.get())
}
""")
                }
            }

            def result = runner.run("build")
            assertThat(result.output)
                    .contains("scminfo.origin = file:" + remoteGitDir.absolutePath + "/.git")
                    .contains("scminfo.source = ")
        })
    }

    @Test
    void test_origin_without_remote() {
        withGit(projectDir, (working) -> {
            def runner = testProject(projectDir) {
                properties {
                    buildCache(true)
                }
                rootProject {
                    plugins {
                        id("com.netflix.nebula.info-scm")
                    }
                    rawBuildScript("""
afterEvaluate {
    println("scminfo.origin = " + scminfo.origin.get())
}
""")
                }
            }

            def result = runner.run("build", "--info")
            assertThat(result.output).contains("scminfo.origin = LOCAL")
        })
    }

    @Test
    void test_origin_without_git() {
        def runner = testProject(projectDir) {
            properties {
                buildCache(true)
            }
            rootProject {
                plugins {
                    id("com.netflix.nebula.info-scm")
                }
                rawBuildScript("""
afterEvaluate {
    println("scminfo.origin = " + scminfo.origin.get())
}
""")
            }
        }

        def result = runner.run("build")
        assertThat(result.output).contains("scminfo.origin = LOCAL")
    }

    @Test
    void test_subproject() {
        withRemoteGit(remoteGitDir, projectDir, (remote, working) -> {
            def runner = testProject(projectDir) {
                properties {
                    buildCache(true)
                }
                rootProject {
                }
                subProject("sub1") {
                    plugins {
                        id("com.netflix.nebula.info-scm")
                    }
                    rawBuildScript("""
afterEvaluate {
    println("scminfo.origin = " + scminfo.origin.get())
    println("scminfo.source = " + scminfo.source.get())
}
""")
                }
            }

            def result = runner.run("build")
            assertThat(result.output)
                    .contains("scminfo.origin = file:" + remoteGitDir.absolutePath + "/.git")
                    .contains("scminfo.source = /sub1")
        })
    }
}
