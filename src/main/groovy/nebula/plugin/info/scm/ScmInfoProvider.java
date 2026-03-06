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

package nebula.plugin.info.scm;

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Could contribute to idea plugin, once we know the right SCM
 */
@NullMarked
public interface ScmInfoProvider {
    /**
     * Determine support. Attempt to not use a library, to reduce impact and side effect of calling
     *
     * @param project project to validate support against
     * @return boolean of the provider's availibility to support the current environment.
     * @deprecated use {@link #supports()} instead
     */
    @Deprecated
    default boolean supports(Project project) {
        return supports();
    }

    boolean supports();

    /**
     * @deprecated Use {@link #source()} instead
     */
    @Deprecated
    @Nullable
    default String calculateSource(Project project) {
        return source().getOrNull();
    }

    /**
     * @deprecated Use {@link #origin()} instead
     */
    @Deprecated
    @Nullable
    default String calculateOrigin(Project project) {
        return origin().getOrNull();
    }

    /**
     * @deprecated Use {@link #change()} instead
     */
    @Deprecated
    @Nullable
    default String calculateChange(Project project) {
        return change().getOrNull();
    }

    /**
     * @deprecated Use {@link #fullChange()} instead
     */
    @Deprecated
    @Nullable
    default String calculateFullChange(Project project) {
        return fullChange().getOrNull();
    }

    /**
     * @deprecated Use {@link #branch()} instead
     */
    @Deprecated
    @Nullable
    default String calculateBranch(Project project) {
        return branch().getOrNull();
    }

    Provider<String> source();

    Provider<String> origin();

    Provider<String> change();

    Provider<String> fullChange();

    Provider<String> branch();
}
