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

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ProviderFactory
import org.jspecify.annotations.NullMarked
import org.jspecify.annotations.Nullable

@NullMarked
abstract class AbstractScmProvider implements ScmInfoProvider {
    /**
     * @deprecated Use {@link #source()} instead
     */
    @Deprecated
    @Nullable
    String calculateModuleSource(File projectDir) {
        source().getOrNull()
    }

    private final ProviderFactory providerFactory

    AbstractScmProvider(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory
    }

    protected ProviderFactory getProviderFactory() {
        return this.providerFactory
    }

    /**
     * when we convert to kotlin, make sure to optimise with tailrec
     */
    @Nullable
    protected RegularFile findFile(Project starting, String filename) {
        RegularFile file = starting.layout.projectDirectory.file(filename)
        if (file.asFile.exists()) {
            println("found" +filename + " at " + file.asFile.absolutePath)
            return file
        }
        if (starting.parent == null) {
            return null
        }
        return findFile(starting.parent, filename)
    }

    /**
     * @deprecated Use {@link #origin()} instead
     */
    @Deprecated
    @Nullable
    String calculateModuleOrigin(File projectDir) {
        return origin().getOrNull()
    }

    /**
     * @deprecated Use {@link #change()} instead
     */
    @Nullable
    @Deprecated
    String calculateChange(File projectDir) {
        change().getOrNull()
    }

    /**
     * @deprecated Use {@link #fullChange()} instead
     */
    @Nullable
    @Deprecated
    String calculateFullChange(File projectDir) {
        return fullChange().getOrNull()
    }

    /**
     * @deprecated Use {@link #branch()} instead
     */
    @Nullable
    @Deprecated
    String calculateBranch(File projectDir) {
        return branch().getOrNull()
    }
}
