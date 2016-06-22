package nebula.plugin.info.dependency

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.dependencies.DependenciesInfoPlugin
import nebula.test.PluginProjectSpec
import org.gradle.api.artifacts.ResolveException
import spock.lang.Unroll

class DependenciesInfoPluginSpec extends PluginProjectSpec {

    @Unroll
    def "dependency resolution results are added as configuration '#conf' is resolved"() {
        setup:
        project.apply plugin: 'java'
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)

        project.with {
            apply plugin: DependenciesInfoPlugin

            repositories {
                mavenCentral()
            }

            configurations { broken }

            dependencies {
                compile 'com.google.guava:guava:18.0'
                testCompile 'junit:junit:4.11'
                broken 'dne:dne:1'
            }
        }

        when:
        try {
            project.configurations.getByName(conf).resolve()
        } catch(ResolveException ignore) {
        }

        def manifest = brokerPlugin.buildManifest()

        then:
        manifest["Resolved-Dependencies-${conf.capitalize()}"] == deps

        where:
        conf            |   deps
        'compile'       |   'com.google.guava:guava:18.0'
        'testCompile'   |   'com.google.guava:guava:18.0,junit:junit:4.11,org.hamcrest:hamcrest-core:1.3'
        'broken'        |   null
    }

    @Override
    String getPluginName() {
        'info-dependencies'
    }
}
