/*
 * Copyright 2014-2020 Netflix, Inc.
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

/**
 * Used to retrieve build information when running on Titus
 * @see <a href="https://netflix.github.io/titus/">https://netflix.github.io/titus/</a>
 * @see <a href="https://github.com/Netflix/titus-control-plane/blob/master/titus-server-master/src/main/java/com/netflix/titus/master/mesos/kubeapiserver/direct/DefaultTaskToPodConverter.jav">https://github.com/Netflix/titus-control-plane/blob/master/titus-server-master/src/main/java/com/netflix/titus/master/mesos/kubeapiserver/direct/DefaultTaskToPodConverter.jav</a>
 */
class TitusProvider extends AbstractContinuousIntegrationProvider {

    TitusProvider(ProviderFactory providerFactory) {
        super(providerFactory)
    }

    @Override
    boolean supports(Project project) {
        getEnvironmentVariable('TITUS_JOB_ID') && getEnvironmentVariable('TITUS_TASK_ID')
    }

    @Override
    String calculateBuildNumber(Project project) {
        getEnvironmentVariable('TITUS_JOB_ID')
    }

    @Override
    String calculateBuildId(Project project) {
        getEnvironmentVariable('TITUS_JOB_ID')
    }

    @Override
    String calculateBuildUrl(Project project) {
        return "${getEnvironmentVariable('NETFLIX_INSTANCE_ID')}/${getEnvironmentVariable('TITUS_JOB_ID')}"
    }

    @Override
    String calculateHost(Project project) {
        getEnvironmentVariable('NETFLIX_INSTANCE_ID')
    }

    @Override
    String calculateJob(Project project) {
        getEnvironmentVariable('NETFLIX_APP')
    }
}
