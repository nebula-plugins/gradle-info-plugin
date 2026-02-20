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

@NullMarked
@CompileStatic
abstract class AbstractContinuousIntegrationProvider implements ContinuousIntegrationInfoProvider {

    protected final ProviderFactory providerFactory

    AbstractContinuousIntegrationProvider(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory
    }

    protected Provider<String> environmentVariable(String envKey) {
        return providerFactory.environmentVariable(envKey)
    }

    protected Provider<String> hostname() {
        return providerFactory.of(HostnameValueSource) {}
    }
}
