package nebula.plugin.info.scm

import nebula.plugin.info.GitSetupLauncherSpec
import nebula.plugin.info.InfoBrokerPlugin
import org.eclipse.jgit.api.RemoteRemoveCommand
import org.gradle.api.plugins.JavaPlugin
import spock.lang.Unroll

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

    @Unroll
    def 'should normalize origin url'() {
        given: "a git repository with a remote origin"
        RemoteRemoveCommand remoteRemoveCommand = git.repository.jgit.remoteRemove()
        remoteRemoveCommand.setRemoteName('origin')
        remoteRemoveCommand.call()
        git.remote.add(name: 'origin', url: url)

        when: "building the project"
        def result = runTasks('build')

        then: "the origin url should be normalized"
        result.standardOutput.contains('Module-Origin:https://something.com/repo.git,')

        where:
        url << [
                'https://something.com/repo.git',
                'https://something.com/repo'
        ]
    }
}
