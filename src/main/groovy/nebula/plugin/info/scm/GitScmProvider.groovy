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

    @Override
    String calculateModuleOrigin(File projectDir) {
        def remoteOriginUrl = executeGitCommand("git", "config", "--get", "remote.origin.url")
        try {
            URL url = remoteOriginUrl.toURL()
            if (url.getUserInfo()) {
                def user = url.getUserInfo().split(":").first()
                url = new URL(url.protocol, user + "@" + url.host, url.port, url.file)
            }
            return url.toExternalForm()
        } catch (MalformedURLException ignore) {
            return remoteOriginUrl
        }
    }

    @Override
    String calculateModuleSource(File projectDir) {
        String gitWorkDir = executeGitCommand("git", "rev-parse", "--show-toplevel")
        return projectDir.absolutePath - new File(gitWorkDir).absolutePath
    }

    @Override
    String calculateChange(File projectDir) {
        return calculateFullChange(projectDir)?.substring(0, 7)
    }

    @Override
    String calculateFullChange(File projectDir) {
        boolean isHashPresent = providerFactory.environmentVariable('GIT_COMMIT').present
        String hash
        if (!isHashPresent) {
            hash = executeGitCommand("git", "rev-parse", "HEAD")
        } else {
            hash = providerFactory.environmentVariable('GIT_COMMIT').get()
        }
        return hash
    }

    @Override
    String calculateBranch(File projectDir) {
        return executeGitCommand("git", "rev-parse", "--abbrev-ref", "HEAD")
    }

    private String executeGitCommand(Object... args) {
        try {
            def execOutput = providerFactory.exec {
                it.commandLine(args)
                it.ignoreExitValue = true
            }
            if(execOutput.result.isPresent() && execOutput.result.get().exitValue != 0) {
                logger.error("Could not execute Git command: ${args.join(' ')} | Reason: ${execOutput.standardError.asText.get()}")
                return null
            }
            return execOutput.standardOutput.asText.get().replaceAll("\n", "").trim()
        } catch (Exception e) {
            logger.error("Could not execute Git command: ${args.join(' ')}")
            return null
        }
    }
}
