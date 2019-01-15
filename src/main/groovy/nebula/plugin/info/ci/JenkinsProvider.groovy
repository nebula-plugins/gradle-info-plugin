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

class JenkinsProvider extends AbstractContinuousIntegrationProvider {

    @Override
    boolean supports(Project project) {
        getEnvironmentVariable('BUILD_NUMBER') && getEnvironmentVariable('JOB_NAME')
    }

    @Override
    String calculateBuildNumber(Project project) {
        getEnvironmentVariable('BUILD_NUMBER')
    }

    @Override
    String calculateBuildId(Project project) {
        getEnvironmentVariable('BUILD_ID')
    }

    @Override
    String calculateHost(Project project) {
        getEnvironmentVariable('JENKINS_URL')
    }

    @Override
    String calculateJob(Project project) {
        getEnvironmentVariable('JOB_NAME')
    }
}
