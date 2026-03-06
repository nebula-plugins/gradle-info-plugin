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

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jspecify.annotations.NullMarked
import org.jspecify.annotations.Nullable

@CompileStatic
@NullMarked
class GitScmProvider extends AbstractScmProvider {
    private boolean supports
    private final GitReadOnlyCommandUtil gitUtil
    private final File projectDir

    GitScmProvider(Project project, ProviderFactory providerFactory) {
        super(providerFactory)
        @Nullable
        File gitDir = findFile(project, ".git")?.asFile
        supports = gitDir != null && gitDir.exists()
        if (supports) {
            gitUtil = GitReadOnlyCommandUtil.create(gitDir.parentFile, providerFactory)
        } else {
            gitUtil = GitReadOnlyCommandUtil.create(project.rootProject.layout.projectDirectory.asFile, providerFactory)
        }
        projectDir = project.layout.projectDirectory.asFile
    }

    @Override
    boolean supports() {
        return supports
    }

    @Override
    Provider<String> origin() {
        return gitUtil.remoteOrigin()
                .map {
                    try {
                        URL url = it.toURL()
                        if (url.getUserInfo()) {
                            def user = url.getUserInfo().split(":").first()
                            url = new URL(url.protocol, user + "@" + url.host, url.port, url.file)
                        }
                        String urlAsExternalForm = url.toExternalForm()
                        return urlAsExternalForm.endsWith('.git') ? url.toExternalForm() : urlAsExternalForm + ".git"
                    } catch (MalformedURLException ignore) {
                        return it
                    }
                }
                .orElse("LOCAL")
    }

    @Override
    Provider<String> source() {
        File root = projectDir
        return gitUtil.moduleSource()
                .map { root.absolutePath - new File(it).absolutePath }
                .orElse(projectDir.absolutePath)
    }

    @Override
    Provider<String> change() {
        return fullChange().map { it.substring(0, 7) }
    }

    @Override
    Provider<String> fullChange() {
        return providerFactory.environmentVariable('GIT_COMMIT')
                .orElse(gitUtil.fullChange())
    }

    @Override
    Provider<String> branch() {
        return gitUtil.currentBranch()
    }
}
