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

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class PerforceScmProviderSpec extends Specification {
    @Rule
    TemporaryFolder temp

    ProviderFactory providerFactoryMock = Mock(ProviderFactory)
    Provider<String> providerStringMock = Mock(Provider)

    def provider = new PerforceScmProvider(providerFactoryMock)

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables()

    def 'lookup settings'() {
        setup:
        def orig = [P4CONFIG: 'p4config', P4USER: 'jryan']
        def dest = [P4USER: 'aeinstein']

        when:
        def result = provider.overrideFromMap(orig, dest)

        then:
        result.P4CONFIG == 'p4config'
        result.P4USER == 'aeinstein'
    }

    def 'find file in project direct'() {
        setup:
        def projectDir = temp.newFolder()
        def deep = new File(projectDir, "level1/level2/level3")
        deep.mkdirs()
        def configName = 'p4config'
        def config = new File(projectDir, configName)
        config.text = "something"

        when:
        def found = provider.findFile(deep, configName)

        then:
        found.exists()
        found == config

        when:
        def notfound = provider.findFile(deep, "fake")

        then:
        notfound == null
    }

    def 'find perforce defaults'() {
        setup:
        def projectDir = temp.newFolder()
        def config = new File(projectDir, 'p4config')
        config.text = "P4CLIENT=jryan_uber\nP4USER=jryan"
        provider.p4configFile = config

        when:
        def foundMap = provider.perforceDefaults(projectDir)

        then:
        foundMap.P4CLIENT == 'jryan_uber'
        foundMap.P4USER == 'jryan'
        foundMap.P4PORT == 'perforce:1666'
    }

    def 'url looks right'() {
        setup:
        def defaults = [P4USER: 'user', P4PORT: 'port']

        when:
        def result = provider.getUrl(defaults)

        then:
        result == 'p4java://port?userName=user'
    }

    def 'calculate module status'() {
        setup:
        def workspace = new File('/Users/jryan/Workspaces/jryan_uber')
        def fakeProjectDir = new File("/Users/jryan/Workspaces/jryan_uber/Tools/nebula-boot")

        environmentVariables.set("P4CONFIG", "test")
        environmentVariables.set("WORKSPACE", workspace.path)


        when:
        String mapped = provider.calculateModuleSource(fakeProjectDir)

        then:
        mapped == '//depot/Tools/nebula-boot'

        2 * providerFactoryMock.environmentVariable('WORKSPACE') >> providerStringMock
        1 * providerStringMock.present >> true
        1 * providerStringMock.get() >> workspace.path

        when:
        String origin = provider.calculateModuleOrigin(fakeProjectDir)

        then:
        origin == 'p4java://perforce:1666?userName=rolem'

        1 * providerFactoryMock.environmentVariable('P4CONFIG') >> providerStringMock
        1 * providerStringMock.get() >> fakeProjectDir.path
    }

    def 'calculate module status - WORKSPACE is null'() {
        setup:
        environmentVariables.set("P4CONFIG", "test")
        environmentVariables.set("WORKSPACE", null)

        def fakeProjectDir = new File("/Users/jryan/Workspaces/jryan_uber/Tools/nebula-boot")

        when:
        String mapped = provider.calculateModuleSource(fakeProjectDir)

        then:
        mapped == '//depot//Users/jryan/Workspaces/jryan_uber/Tools/nebula-boot'

        1 * providerFactoryMock.environmentVariable('WORKSPACE') >> providerStringMock
        1 * providerStringMock.present >> false
        0 * providerStringMock.get()

        when:
        String origin = provider.calculateModuleOrigin(fakeProjectDir)

        then:
        origin == 'p4java://perforce:1666?userName=rolem'

        1 * providerFactoryMock.environmentVariable('P4CONFIG') >> providerStringMock
        1 * providerStringMock.get() >> fakeProjectDir.path
    }
}
