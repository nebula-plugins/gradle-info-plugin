/*
 * Copyright 2014-2016 Netflix, Inc.
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

/**
 * Could contribute to idea plugin, once we know the right SCM
 */
public interface ScmInfoProvider {
    /**
     * Determine support. Attempt to not use a library, to reduce impact and side effect of calling
     * @param project project to validate support against
     * @return boolean of the provider's availibility to support the current environment.
     */
    boolean supports(Project project);
    String calculateSource(Project project);
    String calculateOrigin(Project project);
    String calculateChange(Project project);
    String calculateBranch(Project project);
}
