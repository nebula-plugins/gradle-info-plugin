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

    @Override
    def calculateBranch(File projectDir) {
        return getRepository(projectDir).branch
    }
}
