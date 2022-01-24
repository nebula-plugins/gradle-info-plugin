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

import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult

class InfoPluginIntegrationSpec extends IntegrationSpec {
    def 'it returns build reports at the end of the build'() {
        given:
        buildFile << """
            buildscript {
                repositories {
                   mavenCentral()
                }    
                dependencies {
                    classpath "com.google.guava:guava:21.0"
                }
            }

            ${applyPlugin(InfoPlugin)}
            apply plugin: 'java'
            
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

        when:
        ExecutionResult result = runTasksSuccessfully('assemble')

        then:
        result.standardOutput.contains('{buildscript-singlemodule-test-dependencies={Resolved-Buildscript-Dependencies-Classpath=com.google.guava:guava:21.0, Resolved-Dependencies-CompileClasspath=com.google.guava:guava:18.0}}')
    }

    def 'it returns build reports at the end of multiproject build'() {
        given:
        buildFile << """
            allprojects {
                ${applyPlugin(InfoPlugin)}
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

        when:
        ExecutionResult result = runTasksSuccessfully('build')

        then:
        result.standardOutput.contains('common-dependencies={Resolved-Dependencies-CompileClasspath=com.google.guava:guava:18.0}')
        result.standardOutput.contains('app-dependencies={Resolved-Dependencies-CompileClasspath=com.google.guava:guava:19.0}')
    }

    def 'it returns build reports at the end of multiproject build - with buildscript classpath info'() {
        given:
        buildFile << """
            buildscript {
                repositories {
                   mavenCentral()
                }    
                dependencies {
                    classpath "com.google.guava:guava:21.0"
                }
            }

            allprojects {
                ${applyPlugin(InfoPlugin)}
            }

            subprojects {
                repositories { mavenCentral() }
            }

            def broker = project.plugins.getPlugin(${InfoBrokerPlugin.name})

            gradle.buildFinished {
                println broker.buildReports().get('resolved-dependencies')
            }
        """.stripIndent()

        settingsFile << """
            rootProject.name='buildscript-multimodule-test' 
        """

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

        when:
        ExecutionResult result = runTasksSuccessfully('build')

        then:
        result.standardOutput.contains('buildscript-multimodule-test-dependencies={Resolved-Buildscript-Dependencies-Classpath=com.google.guava:guava:21.0}')
        result.standardOutput.contains('common-dependencies={Resolved-Dependencies-CompileClasspath=com.google.guava:guava:18.0}')
        result.standardOutput.contains('app-dependencies={Resolved-Dependencies-CompileClasspath=com.google.guava:guava:19.0}')
    }

    def 'it returns build reports at the end of multiproject build - with buildscript classpath info - submodule has classpath'() {
        given:
        buildFile << """
            buildscript {
                repositories {
                   mavenCentral()
                }    
                dependencies {
                    classpath "com.google.guava:guava:21.0"
                }
            }

            allprojects {
                ${applyPlugin(InfoPlugin)}
            }

            subprojects {
                repositories { mavenCentral() }
            }

            def broker = project.plugins.getPlugin(${InfoBrokerPlugin.name})

            gradle.buildFinished {
                println broker.buildReports().get('resolved-dependencies')
            }
        """.stripIndent()

        settingsFile << """
            rootProject.name='buildscript-multimodule-test' 
        """

        def common = addSubproject('common', '''\
            apply plugin: 'java'
            dependencies {
                implementation 'com.google.guava:guava:18.0'
            }
            '''.stripIndent())
        writeHelloWorld('nebula.common', common)
        def app = addSubproject('app', '''\
            buildscript {
              repositories {
                maven {
                  url "https://plugins.gradle.org/m2/"
                }
              }
              dependencies {
                classpath "com.github.jengelman.gradle.plugins:shadow:5.2.0"
              }
            }

            apply plugin: 'java'
            dependencies {
                implementation 'com.google.guava:guava:19.0'
            }
            '''.stripIndent())
        writeHelloWorld('nebula.app', app)

        when:
        ExecutionResult result = runTasksSuccessfully('build')

        then:
        result.standardOutput.contains('{buildscript-multimodule-test-dependencies={Resolved-Buildscript-Dependencies-Classpath=com.google.guava:guava:21.0}, app-dependencies={Resolved-Buildscript-Dependencies-Classpath=com.github.jengelman.gradle.plugins:shadow:5.2.0,commons-io:commons-io:2.6,org.apache.ant:ant:1.9.7,org.apache.ant:ant-launcher:1.9.7,org.apache.logging.log4j:log4j-api:2.17.0,org.apache.logging.log4j:log4j-core:2.17.0,org.codehaus.plexus:plexus-utils:3.0.24,org.jdom:jdom2:2.0.6,org.ow2.asm:asm:7.0-beta,org.ow2.asm:asm-analysis:7.0-beta,org.ow2.asm:asm-commons:7.0-beta,org.ow2.asm:asm-tree:7.0-beta,org.vafer:jdependency:2.1.1, Resolved-Dependencies-CompileClasspath=com.google.guava:guava:19.0}, common-dependencies={Resolved-Dependencies-CompileClasspath=com.google.guava:guava:18.0}}')
    }

    def 'works with jenkins jpi plugin'() {
        given:
        buildFile << """
            buildscript {
              repositories {
                maven {
                  url "https://plugins.gradle.org/m2/"
                }
              }
              dependencies {
                classpath "org.jenkins-ci.tools:gradle-jpi-plugin:0.43.0"
              }
            }

            ${applyPlugin(InfoPlugin)}
            apply plugin: 'java'
            apply plugin: "org.jenkins-ci.jpi"

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

        when:
        ExecutionResult result = runTasksSuccessfully('assemble')

        then:
        result.success
    }
}
