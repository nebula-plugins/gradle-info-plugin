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
                id 'com.netflix.nebula.info'
            }
            
             java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                    vendor = JvmVendorSpec.ADOPTIUM
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
        result.output.contains('Build-Java-Version=17')
    }

    def 'reports proper jdk version when configuring toolchain in compile task'() {
        given:
        debug = true
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
                id 'com.netflix.nebula.info'
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                    vendor = JvmVendorSpec.ADOPTIUM
                }
            }

            tasks.withType(JavaCompile).configureEach {
                javaCompiler = javaToolchains.compilerFor {
                    languageVersion = JavaLanguageVersion.of(8)
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
        result.output.contains('Build-Java-Version=17')
       assert result.output.contains('X-Compile-Target-JDK=8') || result.output.contains('X-Compile-Target-JDK=1.8')
        assert result.output.contains('X-Compile-Source-JDK=8') || result.output.contains('X-Compile-Target-JDK=1.8')
    }

    def 'reports proper jdk version when configuring target/source compatibility in compile task + toolchains'() {
        given:
        debug = true
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
                id 'com.netflix.nebula.info'
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                    vendor = JvmVendorSpec.ADOPTIUM
                }
            }

            tasks {
                compileJava {
                    sourceCompatibility = JavaVersion.VERSION_1_8
                    targetCompatibility = JavaVersion.VERSION_1_8
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
        result.output.contains('Build-Java-Version=17')
        result.output.contains('X-Compile-Target-JDK=1.8')
        result.output.contains('X-Compile-Source-JDK=1.8')
    }

    def 'reports proper jdk version when configuring target/source compatibility in compile task + toolchains (multi language)'() {
        given:
        debug = true
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
                id 'groovy'
                id 'com.netflix.nebula.info'
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                    vendor = JvmVendorSpec.ADOPTIUM
                }
            }

            tasks {
                compileJava {
                    sourceCompatibility = JavaVersion.VERSION_1_8
                    targetCompatibility = JavaVersion.VERSION_1_8
                }
                compileGroovy {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
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
        result.output.contains('Build-Java-Version=17')
        result.output.contains('X-Compile-Target-JDK=11')
        result.output.contains('X-Compile-Source-JDK=11')
    }

    def 'reports proper jdk version when configuring target/source compatibility in compile task + toolchains (scala support)'() {
        given:
        debug = true
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
                id 'scala'
                id 'com.netflix.nebula.info'
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                    vendor = JvmVendorSpec.ADOPTIUM
                }
            }

            tasks {
                compileJava {
                    sourceCompatibility = JavaVersion.VERSION_1_8
                    targetCompatibility = JavaVersion.VERSION_1_8
                }
                compileScala {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
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
        result.output.contains('Build-Java-Version=17')
        result.output.contains('X-Compile-Target-JDK=11')
        result.output.contains('X-Compile-Source-JDK=11')
    }

}
