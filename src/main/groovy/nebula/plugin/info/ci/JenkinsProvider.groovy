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

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

class JenkinsProvider extends AbstractContinuousIntegrationProvider {

    JenkinsProvider(ProviderFactory providerFactory) {
        super(providerFactory)
    }

    @Override
    boolean supports() {
        environmentVariable('BUILD_NUMBER').isPresent() && environmentVariable('JOB_NAME').isPresent()
    }

    @Override
    Provider<String> buildNumber() {
        environmentVariable('BUILD_NUMBER')
    }

    @Override
    Provider<String> buildId() {
        environmentVariable('BUILD_ID')
    }

    @Override
    Provider<String> buildUrl() {
        environmentVariable('BUILD_URL')
    }

    @Override
    Provider<String> host() {
        environmentVariable('JENKINS_URL')
    }

    @Override
    Provider<String> job() {
        environmentVariable('JOB_NAME')
    }
}
