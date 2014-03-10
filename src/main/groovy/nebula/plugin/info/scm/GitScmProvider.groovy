package nebula.plugin.info.scm

import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.gradle.api.Project

class GitScmProvider extends AbstractScmProvider {

    @Override
    boolean supports(Project project) {
        // TODO When we can make p4java optional, we'll add a classForName check here.
        return findFile(project.projectDir, '.git') != null
    }

    private Repository getRepository(File projectDir) {
        new RepositoryBuilder().findGitDir(projectDir).build();
    }

    @Override
    def calculateModuleOrigin(File projectDir) {
        Repository repository = getRepository(projectDir)
        Config storedConfig = repository.getConfig();
        String url = storedConfig.getString('remote', 'origin', 'url');
        return url
    }

    @Override
    def calculateModuleSource(File projectDir) {
        Repository repository = getRepository(projectDir)
        def gitDir = repository.directory
        def relative = projectDir.absolutePath - gitDir.parentFile.absolutePath
        return relative
    }

    @Override
    String calculateChange(File projectDir) {
        def hash = System.getenv('GIT_COMMIT') // From Jenkins
        if (hash==null) {
            def head = getRepository(projectDir).resolve(Constants.HEAD)
            if (head==null) {
                return null
            }
            hash = head.name
        }
        def shortHash = hash?.substring(0,7)
        return shortHash
    }

}
