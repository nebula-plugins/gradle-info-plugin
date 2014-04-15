package nebula.plugin.info.dependencies

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.java.InfoJavaPlugin
import nebula.plugin.info.reporting.InfoPropertiesFilePlugin
import nebula.test.IntegrationSpec
import nebula.test.dependencies.DependencyGraph
import nebula.test.dependencies.GradleDependencyGenerator

class DependencyInfoPluginLauncherSpec extends IntegrationSpec {

    def 'resolves status of dependencies'() {
        def propsLocation = "build/manifest/${moduleName}.properties"

        def repoDir = directory('build/testrepogen/')
        //def directory = 'build/testdependencies/mavenpom'
        def graph = ['test.maven:foo:1.0.0-SNAPSHOT', 'test.maven:bar:1.0.0']
        def generator = new GradleDependencyGenerator(new DependencyGraph(graph), repoDir.getCanonicalPath())
        generator.generateTestMavenRepo()

        writeHelloWorld('nebula')
        buildFile << """
            repositories {
                maven { url file('${repoDir}/mavenrepo/') }
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
