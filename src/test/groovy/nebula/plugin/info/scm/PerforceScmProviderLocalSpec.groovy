/*
 * Copyright 2014, 2016 Netflix, Inc.
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

import com.perforce.p4java.client.IClient
import com.perforce.p4java.impl.generic.client.ClientView
import com.perforce.p4java.server.IServer
import nebula.test.ProjectSpec
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class PerforceScmProviderLocalSpec extends ProjectSpec {
    @Rule
    TemporaryFolder testDir

    def provider = new PerforceScmProvider()

    def 'connect to perforce'() {
        when:
        ClientView view = provider.withPerforce(projectDir) { IServer server ->
            IClient client = server.getClient('jryan_uber');
            ClientView view = client.getClientView()
            return view
        }

        then:
        view.size > 5
    }

    def 'calculate module status'() {
        setup:
        def config = new File(projectDir, 'p4config')
        config.text = "P4CLIENT=jryan_uber\nP4USER=jryan"
        provider.p4configFile = config
        def workspace = new File('/Users/jryan/Workspaces/jryan_uber')
        def fakeProjectDir = new File("/Users/jryan/Workspaces/jryan_uber/Tools/nebula-boot")

        when:
        String mapped = provider.calculateModuleSource(workspace, fakeProjectDir)

        then:
        mapped == '//depot/Tools/nebula-boot'

        when:
        String origin = provider.calculateModuleOrigin(fakeProjectDir)

        then:
        origin == 'p4java://perforce:1666?userName=jryan'
    }
}
