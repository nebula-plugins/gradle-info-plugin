/*
 * Copyright 2016-2020 Netflix, Inc.
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

@NullMarked
@CompileStatic
class CirrusCIProvider extends AbstractContinuousIntegrationProvider {
    CirrusCIProvider(ProviderFactory providerFactory) {
        super(providerFactory)
    }

    @Override
    boolean supports() {
        environmentVariable("CIRRUS_CI").isPresent()
    }

    @Override
    Provider<String> host() {
        return hostname()
    }

    @Override
    Provider<String> job() {
        environmentVariable("CIRRUS_REPO_FULL_NAME")
    }

    @Override
    Provider<String> buildNumber() {
        environmentVariable("CIRRUS_CHANGE_IN_REPO")
    }

    @Override
    Provider<String> buildId() {
        environmentVariable("CIRRUS_BUILD_ID")
    }

    @Override
    Provider<String> buildUrl() {
        return host().flatMap { host ->
            buildId().map { id ->
                "${host}/build/${id}".toString()
            }
        }
    }
}
