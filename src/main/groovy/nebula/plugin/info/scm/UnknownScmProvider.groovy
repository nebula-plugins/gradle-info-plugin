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

package nebula.plugin.info.scm

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jspecify.annotations.NullMarked

@NullMarked
@CompileStatic
class UnknownScmProvider extends AbstractScmProvider {

    public static final String LOCAL = 'LOCAL'
    private final Project project

    UnknownScmProvider(Project project, ProviderFactory providerFactory) {
        super(providerFactory)
        this.project = project
    }

    @Override
    boolean supports() {
        return true
    }

    @Override
    Provider<String> origin() {
        return providerFactory.provider {LOCAL}
    }

    @Override
    Provider<String> source() {
        return providerFactory.provider { project.layout.projectDirectory.asFile.absolutePath }
    }

    @Override
    Provider<String> change() {
        return providerFactory.provider { null }
    }

    @Override
    Provider<String> fullChange() {
        return providerFactory.provider { null }
    }

    @Override
    Provider<String> branch() {
        return providerFactory.provider { null }
    }
}
