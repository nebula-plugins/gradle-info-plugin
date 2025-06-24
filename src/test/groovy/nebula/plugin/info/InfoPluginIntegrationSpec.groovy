/*
 * Copyright 2016-2019 Netflix, Inc.
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
package nebula.plugin.info

import spock.lang.Ignore


class InfoPluginIntegrationSpec extends BaseIntegrationTestKitSpec {
    def 'it returns build reports at the end of the build'() {
        given:
        System.setProperty("ignoreDeprecations", 'true')
        buildFile << """
            buildscript {
                repositories {
                   mavenCentral()
                }    
                dependencies {
                    classpath "com.google.guava:guava:21.0"
                }
            }

            plugins {
                id 'com.netflix.nebula.info'
                id 'java'
            }
            
            repositories { mavenCentral() }
            dependencies {
                implementation 'com.google.guava:guava:18.0'
            }
            def broker = project.plugins.getPlugin(${InfoBrokerPlugin.name})

            gradle.buildFinished {
                println broker.buildReports().get('resolved-dependencies')
            }
        """.stripIndent()

        settingsFile << """
            rootProject.name='buildscript-singlemodule-test' 
        """
        this.writeHelloWorld('com.nebula.test')
        new File(projectDir, 'gradle.properties').text = '''org.gradle.configuration-cache=false'''.stripIndent()

        when:
        def result = runTasks('assemble')

        then:
        result.output.contains('{buildscript-singlemodule-test-dependencies={Resolved-Dependencies-CompileClasspath=com.google.guava:guava:18.0}}')
    }

    def 'it returns build reports at the end of multiproject build'() {
        given:
        System.setProperty("ignoreDeprecations", 'true')
        buildFile << """
            plugins {
                id 'com.netflix.nebula.info'
            }
            allprojects {
                apply plugin: 'com.netflix.nebula.info'
            }

            subprojects {
                repositories { mavenCentral() }
            }

            def broker = project.plugins.getPlugin(${InfoBrokerPlugin.name})

            gradle.buildFinished {
                println broker.buildReports().get('resolved-dependencies')
            }
        """.stripIndent()
        def common = addSubproject('common', '''\
            apply plugin: 'java'
            dependencies {
                implementation 'com.google.guava:guava:18.0'
            }
            '''.stripIndent())
        writeHelloWorld('nebula.common', common)
        def app = addSubproject('app', '''\
            apply plugin: 'java'
            dependencies {
                implementation 'com.google.guava:guava:19.0'
            }
            '''.stripIndent())
        writeHelloWorld('nebula.app', app)
        new File(projectDir, 'gradle.properties').text = '''org.gradle.configuration-cache=false'''.stripIndent()

        when:
        def result = runTasks('build')

        then:
        result.output.contains('common-dependencies={Resolved-Dependencies-CompileClasspath=com.google.guava:guava:18.0}')
        result.output.contains('app-dependencies={Resolved-Dependencies-CompileClasspath=com.google.guava:guava:19.0}')
    }

    @Ignore("generateLicenseInfo fails")
    def 'works with jenkins jpi plugin'() {
        given:
        System.setProperty("ignoreDeprecations", 'true')
        buildFile << """
            plugins {
                id 'com.netflix.nebula.info'
                id 'java'
                id "org.jenkins-ci.jpi" version "0.54.0"
            }
           
            jenkinsPlugin {
                jenkinsVersion.set('2.249.3')
            }

            repositories { mavenCentral() }
            dependencies {
                implementation 'com.google.guava:guava:18.0'
            }
        """.stripIndent()

        settingsFile << """
            rootProject.name='test-jenkins-jpi' 
        """
        writeHelloWorld('com.nebula.test')
        // JPI plugin might not be configuration cache compatible yet
        new File(projectDir, 'gradle.properties').text = '''org.gradle.configuration-cache=false'''.stripIndent()

        expect:
        runTasks('assemble')
    }
}
