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

class PerforceScmProvider extends AbstractScmProvider {

    File p4configFile

    @Override
    boolean supports(Project project) {
        // Pretty poor way to check, but Perforce leave no indication of where the current tree came from
        // Better to check git first, since it can make a more intelligent guess
        // TODO When we can make p4java optional, we'll add a classForName check here.
        return (System.getenv('WORKSPACE') && System.getenv('P4CLIENT')) || findFile(project.projectDir, System.getenv('P4CONFIG'))
    }

    @Override
    String calculateModuleSource(File projectDir) {
        def workspace = new File(System.getenv('WORKSPACE'))
        return calculateModuleSource(workspace, projectDir)
    }
    String calculateModuleSource(File workspace, File projectDir) {
        // TODO Don't hardcode depot
        def relativePath = projectDir.getAbsolutePath() - (workspace.getAbsolutePath() + '/')
        return "//depot/${relativePath}"
//        withPerforce(projectDir) { IServer server, IClient client ->
//            def relativePath = projectDir.getAbsolutePath() - (workspace.getAbsolutePath() + '/')
//            List<IFileSpec> specs = FileSpecBuilder.makeFileSpecList(relativePath)
//            List<IFileSpec> mapped = client.where(specs)
//            mapped.each { IFileSpec spec ->
//                println spec.getClientPathString()
//                println spec.getDepotPathString()
//            }
//        }
    }

//    def scmBase() {
//        // TOOD use p4java instead of command line call
//        Process process = "p4 workspace -o".execute(null, project.rootDir)
//        def output = process.text // Potential save for later
//        def matcher = output =~ /Root:\s+(\S+)\n/
//        if(matcher) {
//            return matcher[0][1]
//        }
//
//        return null
//    }
//    /**
//     * macro.xml -> viewFromPath
//     */
//    def viewFromPath(File baseDir, String depot) {
//        // TODO Previously used workspace, which then made a relative path from that location prepended with depot
//        //      We always knew workspace because if it wasn't provided we could
//        baseDir.absoluteFile
//    }


    @Override
    String calculateModuleOrigin(File projectDir) {
        Map<String, String> defaults = perforceDefaults(projectDir)
        return getUrl(defaults)
    }

    @Override
    String calculateChange(File projectDir) {
        return System.getenv('P4_CHANGELIST')
    }

    @Override
    def calculateBranch(File projectDir) {
        return null // unsupported in perforce
    }

    @PackageScope
    <T> T withPerforce(File projectDir, Closure<T> closure) {
        Map<String, String> defaults = perforceDefaults(projectDir)
        def uri = getUrl(defaults)
        IServer server = ServerFactory.getServer(uri, null);
        server.connect()
        if (defaults.P4PASSWD) {
            server.login(defaults.P4PASSWD)
        }

        IClient client
        if (defaults.P4CLIENT) {
            client = server.getClient(defaults.P4CLIENT)
            if (client != null) {
                server.setCurrentClient(client);
            }
        }

        T ret
        try {
            if (closure.maximumNumberOfParameters==1) {
                ret = closure.call(server)
            } else {
                if (client == null) {
                    throw new NullPointerException("P4CLIENT was not specified, but closure is asking for it.")
                }
                ret = closure.call(server, client)
            }
        } finally {
            if (server!=null) {
                server.disconnect()
            }
        }
        return ret
    }

    @PackageScope
    String getUrl(Map<String, String> defaults) {
        // TODO Support SSL
        //return "p4java://${defaults.P4USER}${passAppend}@${defaults.P4PORT}"
        return "p4java://${defaults.P4PORT}?userName=${defaults.P4USER}"
    }

    @PackageScope
    def Map<String, String> perforceDefaults(File projectDir) {
        // Set some default values then look for overrides
        def defaults = [
                P4CLIENT: null,
                P4USER: 'rolem',
                P4PASSWD: '',
                P4PORT: 'perforce:1666'
        ]

        // First look for P4CONFIG name
        findP4Config(projectDir) // Might be noop
        if (p4configFile) {
            def props = new Properties()
            props.load(new FileReader(p4configFile))
            defaults = overrideFromMap(defaults, props)
        }

        // Second user environment variables
        defaults = overrideFromMap(defaults, System.getenv())

        return defaults
    }

    @PackageScope
    def Map<String, String> overrideFromMap(Map<String, String> orig, Map<String, String> override) {
        def dest = [:]
        orig.keySet().each { String key ->
            dest[key] = override.keySet().contains(key) ? override[key] : orig[key]
        }
        return dest
    }

    @PackageScope
    void findP4Config(File starting) {
        if (p4configFile == null) {
            p4configFile = findFile(starting, System.getenv('P4CONFIG'))
        }
    }
}
