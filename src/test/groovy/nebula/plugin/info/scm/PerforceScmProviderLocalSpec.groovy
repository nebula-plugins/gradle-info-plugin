package nebula.plugin.info.scm

import com.energizedwork.spock.extensions.TempDirectory
import com.perforce.p4java.client.IClient
import com.perforce.p4java.impl.generic.client.ClientView
import com.perforce.p4java.server.IServer
import nebula.test.ProjectSpec

class PerforceScmProviderLocalSpec extends ProjectSpec {

    @TempDirectory File projectDir

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
