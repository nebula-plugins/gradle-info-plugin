package nebula.plugin.info.scm

import nebula.plugin.info.GitSetupLauncherSpec
import nebula.plugin.info.InfoBrokerPlugin
import org.gradle.api.plugins.JavaPlugin

class GitScmProviderLauncherSpec extends GitSetupLauncherSpec {
    @Override
    void initializeBuildGradleSettingsGradle() {
        buildFile << """\
            ${applyPlugin(ScmInfoPlugin)}
            ${applyPlugin(InfoBrokerPlugin)}
            ${applyPlugin(JavaPlugin)}

            group = 'test.nebula'

            def broker = project.plugins.getPlugin(${InfoBrokerPlugin.name})
            gradle.buildFinished {
                println "manifest: " + broker.buildManifest()
            }
  
        """.stripIndent()
    }

    def 'when building and Git is present, we should get a full-change attribute'() {
        when:
        def result = runTasks('build')

        then:
        result.standardOutput.contains('Full-Change:')
    }
}
