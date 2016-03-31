package nebula.plugin.info

import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult

class InfoBrokerPluginIntegrationSpec extends IntegrationSpec {

    def 'it returns build reports at the end of the build'() {
        given:
        def report = 'This string may only be retrieved after the build has finished'
        buildFile << """
            ${applyPlugin(InfoBrokerPlugin)}

            def broker = project.plugins.getPlugin(${InfoBrokerPlugin.name})

            gradle.buildFinished {
                println broker.buildReports().get('report')
            }

            task createReport << {
                broker.addReport('report', '$report')
            }

        """.stripIndent()

        when:
        ExecutionResult result = runTasksSuccessfully('createReport')

        then:
        result.standardOutput.contains(report)
    }

}