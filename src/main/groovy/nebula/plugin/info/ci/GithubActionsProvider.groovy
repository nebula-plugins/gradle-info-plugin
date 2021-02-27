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

import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory

class GithubActionsProvider extends AbstractContinuousIntegrationProvider {

    GithubActionsProvider(ProviderFactory providerFactory) {
        super(providerFactory)
    }

    @Override
    boolean supports(Project project) {
        getEnvironmentVariable('CI') && getEnvironmentVariable('GITHUB_ACTION')  && getEnvironmentVariable('GITHUB_RUN_ID')
    }

    /**
     * A unique number for each run of a particular workflow in a repository.
     * This number begins at 1 for the workflow's first run, and increments with each new run. This number does not change if you re-run the workflow run.
     * @param project
     * @return
     */
    @Override
    String calculateBuildNumber(Project project) {
        getEnvironmentVariable('GITHUB_RUN_NUMBER')
    }

    /**
     * A unique number for each run within a repository. This number does not change if you re-run the workflow run.
     * @param project
     * @return
     */
    @Override
    String calculateBuildId(Project project) {
        getEnvironmentVariable('GITHUB_RUN_ID')
    }

    /**
     * Returns the URL of the GitHub server. For example: https://github.com.
     * @param project
     * @return
     */
    @Override
    String calculateHost(Project project) {
        getEnvironmentVariable('GITHUB_SERVER_URL')
    }

    /**
     * We return GITHUB_ACTION: The unique identifier (id) of the action.
     * @param project
     * @return
     */
    @Override
    String calculateJob(Project project) {
        getEnvironmentVariable('GITHUB_ACTION')
    }

    @Override
    String calculateBuildUrl(Project project) {
        return "${getEnvironmentVariable('GITHUB_SERVER_URL')}/${getEnvironmentVariable('GITHUB_REPOSITORY')}/actions/runs/${getEnvironmentVariable('GITHUB_RUN_ID')}"
    }
}
