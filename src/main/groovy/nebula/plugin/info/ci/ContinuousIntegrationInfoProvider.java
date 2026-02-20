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

package nebula.plugin.info.ci;

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Could contribute to idea plugin, once we know the right SCM
 */
@NullMarked
public interface ContinuousIntegrationInfoProvider {
    /**
     * Determine support. Attempt to not use a library, to reduce impact and side effect of calling
     *
     * @param project project to validate support against
     * @return boolean of the provider's availibility to support the current environment.
     */
    @Deprecated
    default boolean supports(Project project) {
        return supports();
    }

    boolean supports();

    @Deprecated
    @Nullable
    default String calculateHost(Project project) {
        return host().getOrNull();
    }

    @Deprecated
    @Nullable
    default String calculateJob(Project project) {
        return job().getOrNull();
    }

    @Deprecated
    @Nullable
    default String calculateBuildNumber(Project project) {
        return buildNumber().getOrNull();
    }

    @Deprecated
    @Nullable
    default String calculateBuildId(Project project) {
        return buildId().getOrNull();
    }

    @Deprecated
    @Nullable
    default String calculateBuildUrl(Project project) {
        return buildUrl().getOrNull();
    }

    Provider<String> host();

    Provider<String> job();

    Provider<String> buildNumber();

    Provider<String> buildId();

    Provider<String> buildUrl();
}
