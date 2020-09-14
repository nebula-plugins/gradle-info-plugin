package nebula.plugin.info.ci

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.reporting.InfoJarManifestPlugin
import nebula.test.IntegrationSpec
import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import spock.lang.Unroll

import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

class ContinuousIntegrationInfoPluginSpec extends IntegrationSpec {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables()

    @Unroll
    def "Populates manifest attributes by continuous integration info plugin - #provider"() {
        given:
        envVariables.each { envVariable ->
            environmentVariables.set(envVariable.key, envVariable.value)
        }

        writeHelloWorld('nebula.test')
        buildFile << """
            ${applyPlugin(InfoBrokerPlugin)}
            ${applyPlugin(ContinuousIntegrationInfoPlugin)}
            ${applyPlugin(InfoJarManifestPlugin)}

            apply plugin: 'java'

            version = '1.0'
        """.stripIndent()

        when:
        runTasksSuccessfully('jar')

        then:
        File jarFile = new File(projectDir, "build/libs/${moduleName}-1.0.jar")
        Manifest manifest = new JarFile(jarFile).manifest
        Attributes attributes = manifest.mainAttributes

        manifestKey(attributes, 'Build-Job') == expectedBuildInfo.get('Build-Job')
        manifestKey(attributes, 'Build-Number') == expectedBuildInfo.get('Build-Number')
        manifestKey(attributes, 'Build-Id') == expectedBuildInfo.get('Build-Id')

        where:
        provider  | envVariables                                                                                                                                                                                                               || expectedBuildInfo
        'Titus'   | [TITUS_JOB_ID: 'cf12af48-e347-11ea-87d0-0242ac130003', TITUS_TASK_ID: 'dbac27de-e347-11ea-87d0-0242ac130003', NETFLIX_INSTANCE_ID: 'my-netflix-instance-id', NETFLIX_APP: 'my-netflix-app', 'Build-Job': 'my-netflix-app'] || ['Build-Job': 'my-netflix-app', 'Build-Number': 'cf12af48-e347-11ea-87d0-0242ac130003', 'Build-Id': 'cf12af48-e347-11ea-87d0-0242ac130003']
        'Drone'   | [DRONE: 'true', DRONE_REPO: 'org/my-repo', DRONE_BUILD_NUMBER: '1', 'Build-Job': 'org/my-repo']                                                                                                                            || ['Build-Job': 'org/my-repo', 'Build-Number': '1', 'Build-Id': '1']
        'Gitlab'  | [GITLAB_CI: 'true', CI_BUILD_NAME: 'org/my-repo', CI_BUILD_ID: '1', 'Build-Job': 'org/my-repo']                                                                                                                            || ['Build-Job': 'org/my-repo', 'Build-Number': '1', 'Build-Id': '1']
        'Jenkins' | [JOB_NAME: 'org/my-repo', BUILD_ID: '1', 'BUILD_NUMBER': '10', 'Build-Job': 'org/my-repo']                                                                                                                                 || ['Build-Job': 'org/my-repo', 'Build-Number': '10', 'Build-Id': '1']
        'Travis'  | [TRAVIS: 'true', TRAVIS_REPO_SLUG: 'org/my-repo', TRAVIS_BUILD_NUMBER: '10', TRAVIS_BUILD_ID: '1', 'Build-Job': 'org/my-repo']                                                                                             || ['Build-Job': 'org/my-repo', 'Build-Number': '10', 'Build-Id': '1']
    }

    private manifestKey(Attributes attributes, String key) {
        attributes.get(new Attributes.Name(key))
    }
}
