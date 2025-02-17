package nebula.plugin.info.scm

import nebula.plugin.info.GitSetupLauncherSpec
import nebula.plugin.info.InfoBrokerPlugin
import org.gradle.api.plugins.JavaPlugin

class GitScmProviderMultiModuleLauncherSpec extends GitSetupLauncherSpec {
    @Override
    void initializeBuildGradleSettingsGradle() {
        addSubproject("a")
        addSubproject("b")
        buildFile << """\
            allprojects {
                ${applyPlugin(ScmInfoPlugin)}
                ${applyPlugin(InfoBrokerPlugin)}
                ${applyPlugin(JavaPlugin)}
    
                group = 'test.nebula'
    
                def broker = project.plugins.getPlugin(${InfoBrokerPlugin.name})
                gradle.buildFinished {
                    println "manifest: " + broker.buildManifest()
                }
            }
  
        """.stripIndent()
        System.setProperty('ignoreDeprecations', 'true')
    }

    def 'subprojects scm information should be collected from root project extension'() {
        when:
        def result = runTasks('build', '-d')

        then:
        result.standardOutput.contains('Project subprojects-scm-information-should-be-collected-from-root-project-extension SCM information is being collected from provider nebula.plugin.info.scm.GitScmProvider')
        result.standardOutput.contains('Project a SCM information is being collected from rootProject extension')
        result.standardOutput.contains('Project b SCM information is being collected from rootProject extension')
    }

    def 'when building and Git is present, we should get a full-change attribute'() {
        when:
        def result = runTasks('build')

        then:
        result.standardOutput.contains('Full-Change:')
    }
}
