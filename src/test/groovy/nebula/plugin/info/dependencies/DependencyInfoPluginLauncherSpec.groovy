package nebula.plugin.info.dependencies

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.java.InfoJavaPlugin
import nebula.plugin.info.reporting.InfoPropertiesFilePlugin
import nebula.test.IntegrationSpec
import nebula.test.dependencies.DependencyGraph
import nebula.test.dependencies.GradleDependencyGenerator

class DependencyInfoPluginLauncherSpec extends IntegrationSpec {

    def mavenRepoDir
    def ivyRepoDir
    def setup() {
        useToolingApi = false // Want breakpoints

        def repoDir = new File('build/statusrepo')
        def graph = ['test.maven:foo:1.0.0-SNAPSHOT', 'test.maven:bar:1.0.0']
        def generator = new GradleDependencyGenerator(new DependencyGraph(graph), repoDir.getCanonicalPath())

        generator.generateTestMavenRepo()
        mavenRepoDir = "${repoDir.absolutePath}/mavenrepo/"
        generator.generateTestIvyRepo()
        ivyRepoDir = "${repoDir.absolutePath}/ivyrepo/"
    }

    def 'resolves status of dependencies'() {
        def propsLocation = "build/manifest/${moduleName}.properties"


        writeHelloWorld('nebula')
        buildFile << """
            repositories {
                maven { url '${mavenRepoDir}' }
            }

            ${applyPlugin(DependencyInfoPlugin)}
            ${applyPlugin(InfoBrokerPlugin)}
            ${applyPlugin(InfoJavaPlugin)}

            ${applyPlugin(InfoPropertiesFilePlugin)}

            apply plugin: 'java'

            dependencies {
                compile 'test.maven:bar:1.0.0'
                compile 'test.maven:foo:1.0.0-SNAPSHOT'
            }
        """.stripIndent()

        when:
        runTasksSuccessfully('writeManifestProperties')

        then: 'tag reflects snapshot'
        fileExists(propsLocation)

        when:
        def props = new Properties()
        file(propsLocation).withInputStream {
            stream -> props.load(stream)
        }

        then: 'see key in manifest'
        props['Status-Minimum'] == 'integration'

    }

    def 'resolves status of dependencies from an Ivy repository'() {
        def propsLocation = "build/manifest/${moduleName}.properties"


        writeHelloWorld('nebula')
        buildFile << """
            repositories {
                ivy {
                    url '${ivyRepoDir}'
                    layout('pattern') {
                        ivy '[organisation]/[module]/[revision]/[module]-[revision]-ivy.[ext]'
                        artifact '[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]'
                        m2compatible = true
                    }
                }
            }

            ${applyPlugin(DependencyInfoPlugin)}
            ${applyPlugin(InfoBrokerPlugin)}
            ${applyPlugin(InfoJavaPlugin)}

            ${applyPlugin(InfoPropertiesFilePlugin)}

            apply plugin: 'java'

            dependencies {
                compile 'test.maven:bar:1.0.0'
                compile 'test.maven:foo:1.0.0-SNAPSHOT'
            }
        """.stripIndent()

        when:
        runTasksSuccessfully('writeManifestProperties')

        then: 'tag reflects snapshot'
        fileExists(propsLocation)

        when:
        def props = new Properties()
        file(propsLocation).withInputStream {
            stream -> props.load(stream)
        }

        then: 'see key in manifest'
        props['Status-Minimum'] == 'integration'

    }
}
