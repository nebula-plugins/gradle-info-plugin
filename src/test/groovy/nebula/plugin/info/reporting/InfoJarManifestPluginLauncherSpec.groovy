package nebula.plugin.info.reporting

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.basic.BasicInfoPlugin
import nebula.test.IntegrationSpec

class InfoJarManifestPluginLauncherSpec extends IntegrationSpec {

    def 'jarManifest task is not up to date'() {
        writeHelloWorld('nebula.test')
        buildFile << """
            ${applyPlugin(InfoBrokerPlugin)}
            ${applyPlugin(BasicInfoPlugin)}
            ${applyPlugin(InfoJarManifestPlugin)}

            apply plugin: 'java'
        """.stripIndent()

        when:
        def result = runTasksSuccessfully('jar')

        then:
        !result.wasUpToDate(':jarManifest')

        when:
        def secondResult = runTasksSuccessfully('jar')

        then:
        !secondResult.wasUpToDate(':jarManifest')
    }
}
