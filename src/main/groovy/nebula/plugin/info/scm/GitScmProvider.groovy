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

import groovy.transform.Memoized
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GitScmProvider extends AbstractScmProvider {
    private Logger logger = LoggerFactory.getLogger(GitScmProvider
    )
    GitScmProvider(ProviderFactory providerFactory) {
        super(providerFactory)
    }

    @Override
    boolean supports(Project project) {
        return findFile(project.projectDir, '.git') != null
    }

    @Memoized
    @Override
    String calculateModuleOrigin(File projectDir) {
        def remoteOriginUrl = executeGitCommand("git", "config", "--get", "remote.origin.url")
        try {
            URL url = remoteOriginUrl.toURL()
            if (url.getUserInfo()) {
                def user = url.getUserInfo().split(":").first()
                url = new URL(url.protocol, user + "@" + url.host, url.port, url.file)
            }
            String urlAsExternalForm = url.toExternalForm()
            return urlAsExternalForm.endsWith('.git') ? url.toExternalForm() : urlAsExternalForm + ".git"
        } catch (MalformedURLException ignore) {
            return remoteOriginUrl
        }
    }

    @Override
    String calculateModuleSource(File projectDir) {
        String gitWorkDir = executeGitCommand("git", "rev-parse", "--show-toplevel")
        if(!gitWorkDir) {
            return projectDir.absolutePath
        }
        return projectDir.absolutePath - new File(gitWorkDir).absolutePath
    }

    @Override
    String calculateChange(File projectDir) {
        return calculateFullChange(projectDir)?.substring(0, 7)
    }

    @Override
    String calculateFullChange(File projectDir) {
        String hash = providerFactory.environmentVariable('GIT_COMMIT').getOrElse(null)
        if (!hash) {
            hash = executeGitCommand("git", "rev-parse", "HEAD")
        }
        return hash
    }

    @Override
    String calculateBranch(File projectDir) {
        return executeGitCommand("git", "rev-parse", "--abbrev-ref", "HEAD")
    }

    private String executeGitCommand(Object... args) {
        try {
            return providerFactory.exec {
                it.commandLine(args)
            }.standardOutput.asText.get().replaceAll("\n", "").trim()
        } catch (Exception e) {
            logger.error("Could not execute Git command: ${args.join(' ')}")
            return null
        }
    }
}
