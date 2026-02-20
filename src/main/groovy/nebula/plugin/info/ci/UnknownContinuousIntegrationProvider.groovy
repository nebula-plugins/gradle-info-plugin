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

class UnknownContinuousIntegrationProvider extends AbstractContinuousIntegrationProvider {
    public static final String LOCAL = 'LOCAL'

    UnknownContinuousIntegrationProvider(ProviderFactory providerFactory) {
        super(providerFactory)
    }

    @Override
    boolean supports() {
        return true
    }

    @Override
    Provider<String> buildUrl() {
        return host().map { "${it}/${LOCAL}".toString() }
    }

    @Override
    Provider<String> buildNumber() {
        return providerFactory.provider { LOCAL }
    }

    @Override
    Provider<String> buildId() {
        return providerFactory.provider { LOCAL }
    }

    @Override
    Provider<String> host() {
        return hostname()
    }

    @Override
    Provider<String> job() {
        return providerFactory.provider { LOCAL }
    }
}
