/*
 * Copyright 2014-2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
