package nebula.plugin.info

import nebula.test.IntegrationTestKitSpec

class InfoPluginConfigurationCacheSpec extends IntegrationTestKitSpec {

    def 'plugin applies with configuration cache'() {
        buildFile << """
        plugins {
            id 'nebula.info'
            id 'java'
        }
        """
        writeHelloWorld('nebula.app')


        when:
        runTasks('--configuration-cache', 'compileJava', '-s')
        def result = runTasks('--configuration-cache', 'compileJava', '-s')

        then:
        result.output.contains('Reusing configuration cache')
    }
}
