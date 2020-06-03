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
import org.gradle.api.provider.ProviderFactory


abstract class AbstractScmProvider implements ScmInfoProvider {
    abstract calculateModuleSource(File projectDir)

    private final ProviderFactory providerFactory

    AbstractScmProvider(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory
    }

    protected ProviderFactory getProviderFactory() {
        return this.providerFactory
    }

    @Override
    String calculateSource(Project project) {
        return calculateModuleSource(project.projectDir)
    }

    protected File findFile(File starting, String filename) {
        // TODO Stop looking when we get to the home directory, to avoid paths which we know aren't a SCM root
        if (!filename) {
            return null
        }

        File dirToLookIn = starting
        while(dirToLookIn) {
            File p4configFile = new File(dirToLookIn, filename)
            if (p4configFile.exists()) {
                return p4configFile
            }
            dirToLookIn = dirToLookIn?.getParentFile()
        }
        return null
    }

    @Override
    String calculateOrigin(Project project) {
        return calculateModuleOrigin(project.projectDir)
    }

    abstract calculateModuleOrigin(File projectDir)

    @Override
    String calculateChange(Project project) {
        return calculateChange(project.projectDir)
    }

    abstract calculateChange(File projectDir)

    @Override
    String calculateBranch(Project project) {
        return calculateBranch(project.projectDir)
    }

    abstract calculateBranch(File projectDir)
}
