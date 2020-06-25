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
import org.gradle.api.provider.ProviderFactory
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions
import org.tmatesoft.svn.core.wc.*

class SvnScmProvider extends AbstractScmProvider {

    @Override
    boolean supports(Project project) {
        // TODO When we can make p4java optional, we'll add a classForName check here.
        return findFile(project.projectDir, '.svn') != null
    }

    private SVNWCClient getWorkingCopyClient() {
        DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
        SVNClientManager clientManager = SVNClientManager.newInstance(options);
        return clientManager.getWCClient()
    }

    private SVNInfo getInfo(File projectDir) {
        return getWorkingCopyClient().doInfo(projectDir, SVNRevision.WORKING)
    }

    @Override
    String calculateModuleOrigin(File projectDir) {
        return getInfo(projectDir).getURL().toString()
    }

    @Override
    String calculateModuleSource(File projectDir) {
        File svnDir = getInfo(projectDir).getWorkingCopyRoot()
        return projectDir.absolutePath - svnDir.parentFile.absolutePath
    }

    @Override
    String calculateChange(File projectDir) {
        String revision = System.getenv('SVN_REVISION') // From Jenkins
        if (!revision) {
            def base = getInfo(projectDir).getRevision()
            if (!base) {
                return null
            }
            revision = base.getNumber()
        }
        return revision
    }


    @Override
    def calculateFullChange(File projectDir) {
        calculateChange(projectDir)
    }

    @Override
    String calculateBranch(File projectDir) {
        return null // unsupported in svn
    }
}
