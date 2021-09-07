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

import nebula.test.ProjectSpec
import org.eclipse.jgit.api.Git
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Issue

class GitScmProviderLocalSpec extends ProjectSpec {
    @Rule TemporaryFolder temp

    def provider = new GitScmProvider()

    def 'calculate module origin and branch'() {
        setup:
        def projectDir = temp.newFolder()
        def repoUrl = 'https://github.com/Netflix/gradle-template.git'

        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(projectDir)
                .call();

        def fakeProjectDir = new File(projectDir, 'gradle/wrapper')
        fakeProjectDir.mkdirs()

        when:
        String mapped = provider.calculateModuleSource(fakeProjectDir)

        then:
        mapped == '/gradle/wrapper'

        when:
        String origin = provider.calculateModuleOrigin(fakeProjectDir)

        then:
        origin == 'https://github.com/Netflix/gradle-template.git'

        when:
        String branch = provider.calculateBranch(fakeProjectDir)

        then:
        branch == 'master'
    }

    @Issue("32")
    def 'strip password from Git repository URL'() {
        setup:
        def projectDir = temp.newFolder()
        def repoUrl = 'https://x-token-auth:{dKkmJHindwdsaw23-dsaHds5-_jjUBF3-S%c9f}@github.com/Netflix/gradle-template.git'

        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(projectDir)
                .call();

        def fakeProjectDir = new File(projectDir, 'gradle/wrapper')
        fakeProjectDir.mkdirs()

        when:
        String mapped = provider.calculateModuleSource(fakeProjectDir)

        then:
        mapped == '/gradle/wrapper'

        when:
        String origin = provider.calculateModuleOrigin(fakeProjectDir)

        then:
        origin == 'https://x-token-auth@github.com/Netflix/gradle-template.git'

        when:
        String branch = provider.calculateBranch(fakeProjectDir)

        then:
        branch == 'master'
    }

    def 'no module origin'() {
        setup:
        def projectDir = temp.newFolder()
        Git.init()
            .setDirectory(projectDir)
            .call()

        def fakeProjectDir = new File(projectDir, 'gradle/wrapper')
        fakeProjectDir.mkdirs()

        when:
        String mapped = provider.calculateModuleSource( fakeProjectDir )

        then:
        mapped == '/gradle/wrapper'

        when:
        String origin = provider.calculateModuleOrigin(fakeProjectDir)

        then:
        origin == null

        when:
        String change = provider.calculateChange(projectDir)

        then:
        change == null
    }
}
