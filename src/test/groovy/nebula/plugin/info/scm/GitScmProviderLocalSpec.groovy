package nebula.plugin.info.scm

import nebula.test.ProjectSpec
import org.eclipse.jgit.api.Git
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class GitScmProviderLocalSpec extends ProjectSpec {
    @Rule TemporaryFolder temp

    def provider = new GitScmProvider()

    def 'calculate module origin and branch'() {
        setup:
        def projectDir = temp.newFolder()
        def repoUrl = 'https://github.com/Netflix/gradle-template.git'

        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(projectDir)
                .call();

        def fakeProjectDir = new File(projectDir, 'gradle/wrapper')
        fakeProjectDir.mkdirs()

        when:
        String mapped = provider.calculateModuleSource(fakeProjectDir)

        then:
        mapped == '/gradle/wrapper'

        when:
        String origin = provider.calculateModuleOrigin(fakeProjectDir)

        then:
        origin == 'https://github.com/Netflix/gradle-template.git'

        when:
        String branch = provider.calculateBranch(fakeProjectDir)

        then:
        branch == 'master'
    }

    def 'no module origin'() {
        setup:
        def projectDir = temp.newFolder()
        Git.init()
            .setDirectory(projectDir)
            .call()

        def fakeProjectDir = new File(projectDir, 'gradle/wrapper')
        fakeProjectDir.mkdirs()

        when:
        String mapped = provider.calculateModuleSource( fakeProjectDir )

        then:
        mapped == '/gradle/wrapper'

        when:
        String origin = provider.calculateModuleOrigin(fakeProjectDir)

        then:
        origin == null

        when:
        String change = provider.calculateChange(projectDir)

        then:
        change == null
    }
}
