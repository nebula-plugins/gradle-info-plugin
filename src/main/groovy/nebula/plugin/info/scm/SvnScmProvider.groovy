package nebula.plugin.info.scm

import org.gradle.api.Project
import org.tmatesoft.svn.core.wc.*

class SvnScmProvider extends AbstractScmProvider {

    @Override
    boolean supports(Project project) {
        def isVersioned = SVNWCUtil.isVersionedDirectory(project.projectDir)
        return isVersioned
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
            def head = getInfo(projectDir).getRevision()
            if (head==null) {
                return null
            }
            revision = head.getNumber()
        }
        return revision
    }

}
