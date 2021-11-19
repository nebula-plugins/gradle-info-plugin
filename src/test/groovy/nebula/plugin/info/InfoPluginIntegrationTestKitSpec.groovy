/*
 * Copyright 2021 Netflix, Inc.
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

import nebula.test.IntegrationTestKitSpec
import spock.lang.Ignore

@Ignore
class InfoPluginIntegrationTestKitSpec extends IntegrationTestKitSpec {
    def 'it returns manifest reports at the end of the build - toolchains'() {
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

            plugins {
                id 'java'
                id 'nebula.info'
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(16)
                    vendor = JvmVendorSpec.ADOPTOPENJDK
                }
            }

            repositories { mavenCentral() }
            dependencies {
                implementation 'com.google.guava:guava:18.0'
            }
            def broker = project.plugins.getPlugin(${InfoBrokerPlugin.name})

            gradle.buildFinished {
                println broker.buildManifest()
            }
        """.stripIndent()

        settingsFile << """
            rootProject.name='buildscript-singlemodule-test' 
        """
        this.writeHelloWorld('com.nebula.test')

        when:
        def result = runTasks('assemble')

        then:
        println result.output
        result.output.contains('Build-Java-Version=16')
    }

}
