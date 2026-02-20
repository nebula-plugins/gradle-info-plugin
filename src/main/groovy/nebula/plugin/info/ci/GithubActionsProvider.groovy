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

package nebula.plugin.info.ci

import groovy.transform.CompileStatic
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jspecify.annotations.NullMarked

@CompileStatic
@NullMarked
class GithubActionsProvider extends AbstractContinuousIntegrationProvider {

    GithubActionsProvider(ProviderFactory providerFactory) {
        super(providerFactory)
    }

    @Override
    boolean supports() {
        environmentVariable('CI').isPresent() && environmentVariable('GITHUB_ACTION').isPresent() && environmentVariable('GITHUB_RUN_ID').isPresent()
    }

    /**
     * A unique number for each run of a particular workflow in a repository.
     * This number begins at 1 for the workflow's first run, and increments with each new run. This number does not change if you re-run the workflow run.
     * @param project
     * @return
     */
    @Override
    Provider<String> buildNumber() {
        environmentVariable('GITHUB_RUN_NUMBER')
    }

    /**
     * A unique number for each run within a repository. This number does not change if you re-run the workflow run.
     * @param project
     * @return
     */
    @Override
    Provider<String> buildId() {
        environmentVariable('GITHUB_RUN_ID')
    }

    /**
     * Returns the URL of the GitHub server. For example: https://github.com.
     * @param project
     * @return
     */
    @Override
    Provider<String> host() {
        environmentVariable('GITHUB_SERVER_URL')
    }

    /**
     * We return GITHUB_ACTION: The unique identifier (id) of the action.
     * @param project
     * @return
     */
    @Override
    Provider<String> job() {
        environmentVariable('GITHUB_ACTION')
    }

    @Override
    Provider<String> buildUrl() {
        return environmentVariable('GITHUB_SERVER_URL')
                .flatMap { host ->
                    environmentVariable("GITHUB_REPOSITORY")
                            .flatMap { repo ->
                                environmentVariable("GITHUB_RUN_ID")
                                        .map { runId -> "${host}/${repo}/actions/runs/${runId}".toString() }
                            }
                }
    }
}
