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

import com.perforce.p4java.client.IClient
import com.perforce.p4java.server.IServer
import com.perforce.p4java.server.ServerFactory
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

class PerforceScmProvider extends AbstractScmProvider {
    private static final String DEFAULT_WORKSPACE = "."
    private final File projectDir
    private final Provider<RegularFile> configFile

    PerforceScmProvider(Project project, ProviderFactory providerFactory) {
        super(providerFactory)
        this.projectDir = project.layout.projectDirectory.asFile
        configFile = providerFactory.environmentVariable("P4CONFIG").map {
            findFile(project, it)
        }
    }

    @Override
    boolean supports() {
        // Pretty poor way to check, but Perforce leave no indication of where the current tree came from
        // Better to check git first, since it can make a more intelligent guess
        // TODO When we can make p4java optional, we'll add a classForName check here.
        try {
            boolean hasWorkspaceAndClient = providerFactory.environmentVariable('WORKSPACE').present &&
                    providerFactory.environmentVariable('P4CLIENT').present
            boolean hasP4ConfigFile = configFile.map {it.asFile.exists()}.getOrElse(false)
            return hasWorkspaceAndClient || hasP4ConfigFile
        } catch (Exception e) {
            return false
        }
    }

    @Override
    Provider<String> source() {
        File root = projectDir
        return providerFactory.environmentVariable("WORKSPACE")
                .map { workspace -> root.relativePath(new File(workspace)) + "/" }
                .map { "//depot/${it}" }
                .orElse(DEFAULT_WORKSPACE)
    }

    @Override
    Provider<String> origin() {
        return getUrl()
    }

    @Override
    Provider<String> change() {
        return providerFactory.environmentVariable("P4_CHANGELIST")
    }

    @Override
    Provider<String> fullChange() {
        return change()
    }

    @Override
    Provider<String> branch() {
        return providerFactory.provider { null }
    }

    @PackageScope
    <T> T withPerforce(File projectDir, Closure<T> closure) {
        String uri = getUrl()
        IServer server = ServerFactory.getServer(uri, null)
        server.connect()
        if (getPassword().isPresent() && !getPassword().get().isBlank()) {
            server.login(getPassword().get())
        }

        IClient client
        if (getClient().isPresent()) {
            client = server.getClient(getClient().get())
            if (client != null) {
                server.setCurrentClient(client)
            }
        }

        T ret
        try {
            if (closure.maximumNumberOfParameters == 1) {
                ret = closure.call(server)
            } else {
                if (client == null) {
                    throw new NullPointerException("P4CLIENT was not specified, but closure is asking for it.")
                }
                ret = closure.call(server, client)
            }
        } finally {
            if (server != null) {
                server.disconnect()
            }
        }
        return ret
    }

    private Provider<String> getPort() {
        configFile.flatMap {
            providerFactory.fileContents(it).asBytes.map {
                Properties props = new Properties()
                try (ByteArrayInputStream is = new ByteArrayInputStream(it)) {
                    props.load(is)
                }
                return props.get("P4PORT")
            }
        }.orElse(providerFactory.environmentVariable("P4PORT"))
                .orElse('perforce:1666')
    }

    private Provider<String> getUser() {
        configFile.flatMap {
            providerFactory.fileContents(it).asBytes.map {
                Properties props = new Properties()
                try (ByteArrayInputStream is = new ByteArrayInputStream(it)) {
                    props.load(is)
                }
                return props.get("P4USER")
            }
        }.orElse(providerFactory.environmentVariable("P4USER"))
                .orElse("rolem")
    }

    private Provider<String> getClient() {
        configFile.flatMap {
            providerFactory.fileContents(it).asBytes.map {
                Properties props = new Properties()
                try (ByteArrayInputStream is = new ByteArrayInputStream(it)) {
                    props.load(is)
                }
                return props.get("P4CLIENT")
            }
        }.orElse(providerFactory.environmentVariable("P4CLIENT"))
                .orElse(null)
    }

    private Provider<String> getPassword() {
        configFile.flatMap {
            providerFactory.fileContents(it).asBytes.map { bytes ->
                Properties props = new Properties()
                try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
                    props.load(is)
                }
                return props.get("P4PASSWD")
            }
        }.orElse(providerFactory.environmentVariable("P4PASSWD"))
                .orElse("")
    }

    private Provider<String> getUrl() {
        getPort().flatMap { port ->
            getUser().map { user ->
                return "p4java://${port}?userName=${user}"
            }
        }
    }
}
