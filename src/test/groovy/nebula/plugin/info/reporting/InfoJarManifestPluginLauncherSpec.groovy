/*
 * Copyright 2014-2016 Netflix, Inc.
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

package nebula.plugin.info.reporting

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.basic.BasicInfoPlugin
import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult

import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

class InfoJarManifestPluginLauncherSpec extends IntegrationSpec {

    def 'jarManifest task is marked UP-TO-DATE if ran before successfully'() {
        writeHelloWorld('nebula.test')
        buildFile << """
            ${applyPlugin(InfoBrokerPlugin)}
            ${applyPlugin(BasicInfoPlugin)}
            ${applyPlugin(InfoJarManifestPlugin)}

            apply plugin: 'java'
            status = 'release'
        """.stripIndent()

        when:
        // Make sure we have some history already in place
        runTasksSuccessfully('jar')
        runTasksSuccessfully('clean')

        def result = runTasksSuccessfully('jar')
        File jarFile = new File(projectDir, "build/libs/${moduleName}.jar")

        then:
        jarFile.exists()
        !result.wasUpToDate(':jar')
        Manifest manifest = new JarFile(jarFile).manifest
        Attributes attributes = manifest.mainAttributes
        manifestKey(attributes, 'Built-Status') == 'release'

        when: 'Nothing has changed'
        def secondResult = runTasksSuccessfully('jar')

        then:
        secondResult.wasUpToDate(':jar')

        when: 'A manifest field was changed'
        buildFile << """
        status = 'integration'
        """.stripIndent()
        def thirdResult = runTasksSuccessfully('jar')

        then:
        !thirdResult.wasUpToDate(':jar')
        Manifest manifestSnapshot = new JarFile(jarFile).manifest
        Attributes attributesSnapshot = manifestSnapshot.mainAttributes
        manifestKey(attributesSnapshot, 'Built-Status') == 'integration'
    }

    def "Creates JAR file with populated manifest attributes by basic info plugin"() {
        given:
        writeHelloWorld('nebula.test')
        buildFile << """
            ${applyPlugin(InfoBrokerPlugin)}
            ${applyPlugin(BasicInfoPlugin)}
            ${applyPlugin(InfoJarManifestPlugin)}

            apply plugin: 'java'

            version = '1.0'
        """.stripIndent()

        when:
        runTasksSuccessfully('jar')

        then:
        File jarFile = new File(projectDir, "build/libs/${moduleName}-1.0.jar")
        jarFile.exists()
        Manifest manifest = new JarFile(jarFile).manifest
        Attributes attributes = manifest.mainAttributes
        assertMainfestKeyExists(attributes, 'Manifest-Version')
        assertMainfestKeyExists(attributes, 'Implementation-Title')
        assertMainfestKeyExists(attributes, 'Implementation-Version')
        assertMainfestKeyExists(attributes, 'Built-Status')
        assertMainfestKeyExists(attributes, 'Built-By')
        assertMainfestKeyExists(attributes, 'Built-OS')
        assertMainfestKeyExists(attributes, 'Build-Date')
        assertMainfestKeyExists(attributes, 'Gradle-Version')
    }

    def "Creates JAR file with populated manifest excluding properties in infoBrokerPlugin configuration"() {
        given:
        writeHelloWorld('nebula.test')
        buildFile << """
            ${applyPlugin(InfoBrokerPlugin)}
            ${applyPlugin(BasicInfoPlugin)}
            ${applyPlugin(InfoJarManifestPlugin)}

            apply plugin: 'java'

            version = '1.0'

            infoBrokerPlugin {
                excludedProperties = ['Build-Date', 'Built-OS']
            }
        """.stripIndent()

        when:
        runTasksSuccessfully('jar')

        then:
        File jarFile = new File(projectDir, "build/libs/${moduleName}-1.0.jar")
        jarFile.exists()
        Manifest manifest = new JarFile(jarFile).manifest
        Attributes attributes = manifest.mainAttributes
        !attributes.containsKey(new Attributes.Name('Build-Date'))
        !attributes.containsKey(new Attributes.Name('Build-OS'))
        assertMainfestKeyExists(attributes, 'Manifest-Version')
        assertMainfestKeyExists(attributes, 'Implementation-Title')
        assertMainfestKeyExists(attributes, 'Implementation-Version')
        assertMainfestKeyExists(attributes, 'Built-Status')
        assertMainfestKeyExists(attributes, 'Built-By')
        assertMainfestKeyExists(attributes, 'Gradle-Version')
    }

    def "Creates JAR file with populated manifest including only properties in infoBrokerPlugin configuration"() {
        given:
        writeHelloWorld('nebula.test')
        buildFile << """
            ${applyPlugin(InfoBrokerPlugin)}
            ${applyPlugin(BasicInfoPlugin)}
            ${applyPlugin(InfoJarManifestPlugin)}

            apply plugin: 'java'

            version = '1.0'

            infoBrokerPlugin {
                includedProperties = ['Build-Date', 'Built-OS']
            }
        """.stripIndent()

        when:
        runTasksSuccessfully('jar')

        then:
        File jarFile = new File(projectDir, "build/libs/${moduleName}-1.0.jar")
        jarFile.exists()
        Manifest manifest = new JarFile(jarFile).manifest
        Attributes attributes = manifest.mainAttributes
        assertMainfestKeyExists(attributes, 'Build-Date')
        assertMainfestKeyExists(attributes, 'Built-OS')
        !attributes.containsKey(new Attributes.Name('Implementation-Title'))
        !attributes.containsKey(new Attributes.Name('Built-Status'))
        !attributes.containsKey(new Attributes.Name('Built-By'))
        !attributes.containsKey(new Attributes.Name('Gradle-Version'))
    }

    def "Creates JAR fails if includedProperties and excludedProperties are both provided"() {
        given:
        writeHelloWorld('nebula.test')
        buildFile << """
            ${applyPlugin(InfoBrokerPlugin)}
            ${applyPlugin(BasicInfoPlugin)}
            ${applyPlugin(InfoJarManifestPlugin)}

            apply plugin: 'java'

            version = '1.0'

            infoBrokerPlugin {
                includedProperties = ['Build-Date', 'Built-OS']
                excludedProperties = ['Build-Date', 'Built-OS']
            }
        """.stripIndent()

        when:
        ExecutionResult executionResult = runTasksWithFailure('jar')

        then:
        executionResult.standardOutput.contains('includedProperties and excludedProperties are mutually exclusive. Only one should be provided')
    }


    def "changes to group and version are reflected"() {
        given:
        writeHelloWorld('nebula.test')
        buildFile << """
            ${applyPlugin(InfoBrokerPlugin)}
            ${applyPlugin(BasicInfoPlugin)}
            ${applyPlugin(InfoJarManifestPlugin)}

            apply plugin: 'java'

            group = 'com.netflix'
            version = '1.0'
        """.stripIndent()

        when:
        runTasksSuccessfully('jar')

        then:
        File jarFile = new File(projectDir, "build/libs/${moduleName}-1.0.jar")
        jarFile.exists()
        Manifest manifest = new JarFile(jarFile).manifest
        Attributes attributes = manifest.mainAttributes
        attributes.getValue('Implementation-Title') == "com.netflix#changes-to-group-and-version-are-reflected;1.0"
    }

    def "changes to group and version after project evaluation are reflected"() {
        given:
        writeHelloWorld('nebula.test')
        buildFile << """
            ${applyPlugin(InfoBrokerPlugin)}
            ${applyPlugin(BasicInfoPlugin)}
            ${applyPlugin(InfoJarManifestPlugin)}

            apply plugin: 'java'

            afterEvaluate {
                group = 'com.netflix'
                version = '1.0'
            }
        """.stripIndent()

        when:
        runTasksSuccessfully('jar')

        then:
        File jarFile = new File(projectDir, "build/libs/${moduleName}-1.0.jar")
        jarFile.exists()
        Manifest manifest = new JarFile(jarFile).manifest
        Attributes attributes = manifest.mainAttributes
        attributes.getValue('Implementation-Title') == "com.netflix#changes-to-group-and-version-after-project-evaluation-are-reflected;1.0"
    }

    private void assertMainfestKeyExists(Attributes attributes, String key) {
        assert attributes.containsKey(new Attributes.Name(key))
    }

    private manifestKey(Attributes attributes, String key) {
        attributes.get(new Attributes.Name(key))
    }
}
