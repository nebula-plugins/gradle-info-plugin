package nebula.plugin.info.dependencies

import nebula.plugin.info.InfoBrokerPlugin
import nebula.test.PluginProjectSpec
import org.gradle.api.plugins.JavaPlugin

class DependencyInfoPluginSpec extends PluginProjectSpec {
    @Override
    String getPluginName() {
        return 'info-dependency'
    }

    def 'does not add value if configuration is not resolved'() {
        when:
        project.plugins.apply(DependencyInfoPlugin)
        def broker = project.plugins.apply(InfoBrokerPlugin)

        then:
        noExceptionThrown()

        when:
        broker.buildEntry('Status-Minimum')

        then: 'entry should not exist'
        thrown(IllegalArgumentException)
    }

    def 'resolves status of no dependencies'() {
        when:
        project.plugins.apply(DependencyInfoPlugin)
        project.plugins.apply(JavaPlugin) // to create configurations
        def broker = project.plugins.apply(InfoBrokerPlugin)
        def runtimeConfiguration = project.configurations.getByName('runtime')
        runtimeConfiguration.resolve()

        then:
        broker.buildEntry('Status-Minimum') == 'release'
    }
}
