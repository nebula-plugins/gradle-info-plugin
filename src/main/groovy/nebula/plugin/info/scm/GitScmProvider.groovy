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
import org.gradle.api.provider.ProviderFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GitScmProvider extends AbstractScmProvider {
    private Logger logger = LoggerFactory.getLogger(GitScmProvider)

    GitScmProvider(ProviderFactory providerFactory) {
        super(providerFactory)
    }

    @Override
    boolean supports(Project project) {
        // TODO When we can make p4java optional, we'll add a classForName check here.
        return findFile(project.projectDir, '.git') != null
    }

    private Repository getRepository(File projectDir) {
        new RepositoryBuilder().findGitDir(projectDir).build();
    }

    @Override
    String calculateModuleOrigin(File projectDir) {
        Repository repository = getRepository(projectDir)
        Config storedConfig = repository.getConfig()
        String url = storedConfig.getString('remote', 'origin', 'url')
        if (url?.startsWith("https://") || url?.startsWith("http://")) {
            url = hideSensitiveInformation(url)
        }
        return  url
    }

    @Override
    String calculateModuleSource(File projectDir) {
        Repository repository = getRepository(projectDir)
        File gitDir = repository.directory
        return projectDir.absolutePath - gitDir.parentFile.absolutePath
    }

    @Override
    String calculateChange(File projectDir) {
        return calculateFullChange(projectDir)?.substring(0,7)
    }

    @Override
    String calculateFullChange(File projectDir) {
        boolean isHashPresent = providerFactory.environmentVariable('GIT_COMMIT').forUseAtConfigurationTime().present
        String hash
        if (!isHashPresent) {
            def head = getRepository(projectDir).resolve(Constants.HEAD)
            if (!head) {
                return null
            }
            hash = head.name
        } else {
            hash = providerFactory.environmentVariable('GIT_COMMIT').forUseAtConfigurationTime().get()
        }
        return hash
    }

    @Override
    String calculateBranch(File projectDir) {
        return getRepository(projectDir).branch
    }

    private String hideSensitiveInformation(String url) {
        try {
            String credentials = url.toURL().getUserInfo()
            if (credentials) {
                return url.replaceFirst(credentials, credentials.replaceFirst(/:.*/, ""))
            }
        } catch (Exception e) {
            logger.warn("Unable to remove credentials from repository URL. {0}", e.getMessage())
        }
        return url
    }
}
