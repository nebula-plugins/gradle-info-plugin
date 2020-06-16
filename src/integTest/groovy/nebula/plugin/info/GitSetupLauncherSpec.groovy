package nebula.plugin.info

import nebula.test.IntegrationSpec
import org.ajoberstar.grgit.Grgit

import java.nio.file.Files

/**
 * Leverage open source spec, and make some Netflix specific bit available.
 */
abstract class GitSetupLauncherSpec extends IntegrationSpec {
    public Grgit git
    public Grgit originGit

    abstract void initializeBuildGradleSettingsGradle()

    def setup() {
        def origin = new File(projectDir.parent, "${projectDir.name}.git")
        if (origin.exists()) {
            origin.deleteDir()
        }
        origin.mkdirs()

        ['build.gradle', 'settings.gradle'].each {
            Files.move(new File(projectDir, it).toPath(), new File(origin, it).toPath())
        }

        originGit = Grgit.init(dir: origin)
        originGit.add(patterns: ['build.gradle', 'settings.gradle', '.gitignore'] as Set)
        originGit.commit(message: 'Initial checkout')

        git = Grgit.clone(dir: projectDir, uri: origin.absolutePath) as Grgit

        new File(projectDir, '.gitignore') << '''
            .gradle-test-kit
            .gradle
            build/
            gradle.properties
        '''.stripIndent()

        initializeBuildGradleSettingsGradle()

        git.add(patterns: ['build.gradle', 'settings.gradle', '.gitignore'])
        git.commit(message: 'Setup')
        git.push()
    }

    def cleanup() {
        if (git) git.close()
        if (originGit) originGit.close()
    }
}
