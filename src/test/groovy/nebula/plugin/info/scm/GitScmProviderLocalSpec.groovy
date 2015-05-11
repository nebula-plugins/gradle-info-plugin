package nebula.plugin.info.scm

import nebula.test.ProjectSpec
import org.eclipse.jgit.api.Git

class GitScmProviderLocalSpec extends ProjectSpec {
    def provider = new GitScmProvider()

    def 'calculate module origin'() {
        setup:
        def repoUrl = 'https://github.com/Netflix/gradle-template.git'

        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(projectDir)
                .call();

        def fakeProjectDir = new File(projectDir, 'gradle/wrapper')
        fakeProjectDir.mkdirs()

        when:
        String mapped = provider.calculateModuleSource( fakeProjectDir )

        then:
        mapped == '/gradle/wrapper'

        when:
        String origin = provider.calculateModuleOrigin( fakeProjectDir)

        then:
        origin == 'https://github.com/Netflix/gradle-template.git'
    }

    def 'no module origin'() {
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
