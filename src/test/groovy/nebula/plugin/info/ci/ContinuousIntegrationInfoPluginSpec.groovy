package nebula.plugin.info.ci

import nebula.plugin.info.BaseIntegrationTestKitSpec
import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import spock.lang.IgnoreIf
import spock.lang.Unroll

import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

@IgnoreIf({ System.getenv('TITUS_TASK_ID') || jvm.isJava21() || jvm.isJava17() })
class ContinuousIntegrationInfoPluginSpec extends BaseIntegrationTestKitSpec {

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
            plugins {
                id 'com.netflix.nebula.info-broker'
                id 'com.netflix.nebula.info-ci'
                id 'com.netflix.nebula.info-jar'
            }

            apply plugin: 'java'

            version = '1.0'
        """.stripIndent()

        when:
        runTasks('jar')

        then:
        File jarFile = new File(projectDir, "build/libs/${moduleName}-1.0.jar")
        Manifest manifest = new JarFile(jarFile).manifest
        Attributes attributes = manifest.mainAttributes

        manifestKey(attributes, 'Build-Job') == expectedBuildInfo.get('Build-Job')
        manifestKey(attributes, 'Build-Number') == expectedBuildInfo.get('Build-Number')
        manifestKey(attributes, 'Build-Id') == expectedBuildInfo.get('Build-Id')
        manifestKey(attributes, 'Build-Url') == expectedBuildInfo.get('Build-Url')

        where:
        provider         | envVariables                                                                                                                                                                                                               || expectedBuildInfo
        'Titus'          | [TITUS_JOB_ID: 'cf12af48-e347-11ea-87d0-0242ac130003', TITUS_TASK_ID: 'dbac27de-e347-11ea-87d0-0242ac130003', NETFLIX_INSTANCE_ID: 'my-netflix-instance-id', NETFLIX_APP: 'my-netflix-app', 'Build-Job': 'my-netflix-app'] || ['Build-Job': 'my-netflix-app', 'Build-Number': 'cf12af48-e347-11ea-87d0-0242ac130003', 'Build-Id': 'cf12af48-e347-11ea-87d0-0242ac130003', 'Build-Url': 'my-netflix-instance-id/cf12af48-e347-11ea-87d0-0242ac130003']
        'Drone'          | [DRONE: 'true', DRONE_REPO: 'org/my-repo', DRONE_BUILD_NUMBER: '1', 'Build-Job': 'org/my-repo']                                                                                                                            || ['Build-Job': 'org/my-repo', 'Build-Number': '1', 'Build-Id': '1', 'Build-Url': "http://${AbstractContinuousIntegrationProvider.hostname()}/build/1"]
        'Jenkins'        | [JOB_NAME: 'org/my-repo', BUILD_ID: '1', 'BUILD_NUMBER': '10', 'Build-Job': 'org/my-repo', 'BUILD_URL': 'http://localhost/org/my-repo/10']                                                                                 || ['Build-Job': 'org/my-repo', 'Build-Number': '10', 'Build-Id': '1', 'Build-Url': 'http://localhost/org/my-repo/10']
        'Github Actions' | [CI: 'true', GITHUB_ACTION: 'my-action', GITHUB_REPOSITORY: 'org/my-repo', GITHUB_RUN_NUMBER: '10', GITHUB_RUN_ID: '1', 'GITHUB_SERVER_URL': 'http://some-github']                                                         || ['Build-Job': 'my-action', 'Build-Number': '10', 'Build-Id': '1', 'Build-Url': 'http://some-github/org/my-repo/actions/runs/1']
    }

    private manifestKey(Attributes attributes, String key) {
        attributes.get(new Attributes.Name(key))
    }
}
