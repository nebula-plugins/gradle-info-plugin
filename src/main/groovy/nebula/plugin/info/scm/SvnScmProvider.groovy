package nebula.plugin.info.scm

import org.gradle.api.Project
import org.tmatesoft.svn.core.wc.*

class SvnScmProvider extends AbstractScmProvider {

    @Override
    boolean supports(Project project) {
        // TODO When we can make p4java optional, we'll add a classForName check here.
        return findFile(project.projectDir, '.svn') != null
    }

    private SVNWCClient getWorkingCopyClient() {
        def options = SVNWCUtil.createDefaultOptions(true);
        def clientManager = SVNClientManager.newInstance(options);
        def client = clientManager.getWCClient()
        return client
    }

    private SVNInfo getInfo(File projectDir) {
        def info = getWorkingCopyClient().doInfo(projectDir, SVNRevision.WORKING)
        return info
    }

    @Override
    def calculateModuleOrigin(File projectDir) {
        def url = getInfo(projectDir).getURL().toString()
        return url
    }

    @Override
    def calculateModuleSource(File projectDir) {
        def svnDir = getInfo(projectDir).getWorkingCopyRoot()
        def relative = projectDir.absolutePath - svnDir.parentFile.absolutePath
        return relative
    }

    @Override
    String calculateChange(File projectDir) {
        def revision = System.getenv('SVN_REVISION') // From Jenkins
        if (revision==null) {
            def base = getInfo(projectDir).getRevision()
            if (base==null) {
                return null
            }
            revision = base.getNumber()
        }
        return revision
    }

    @Override
    def calculateBranch(File projectDir) {
        return null // unsupported in svn
    }
}
