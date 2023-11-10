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

package nebula.plugin.info.reporting

import nebula.plugin.info.BaseIntegrationTestKitSpec
import org.gradle.testkit.runner.TaskOutcome

import java.time.OffsetDateTime
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

class InfoJarManifestPluginLauncherSpec extends BaseIntegrationTestKitSpec {

    def 'jar task is marked UP-TO-DATE if ran before successfully and manifest changes are ignored'() {
        writeHelloWorld('nebula.test')
        buildFile << """
            plugins {
                id 'com.netflix.nebula.info-broker'
                id 'com.netflix.nebula.info-basic'
                id 'com.netflix.nebula.info-jar'
            }

            apply plugin: 'java'
            status = 'release'
        """.stripIndent()

        when:
        // Make sure we have some history already in place
        runTasks('jar')
        runTasks('clean')

        def result = runTasks('jar')
        File jarFile = new File(projectDir, "build/libs/${moduleName}.jar")

        then:
        jarFile.exists()
        result.task(':jar').outcome != TaskOutcome.UP_TO_DATE
        Manifest manifest = new JarFile(jarFile).manifest
        Attributes attributes = manifest.mainAttributes
        manifestKey(attributes, 'Built-Status') == 'release'

        when: 'Nothing has changed'
        def secondResult = runTasks('jar')

        then:
        secondResult.task(':jar').outcome == TaskOutcome.UP_TO_DATE

        when: 'A manifest field was changed'
        buildFile << """
        status = 'integration'
        """.stripIndent()
        def thirdResult = runTasks('jar')

        then:
        secondResult.task(':jar').outcome == TaskOutcome.UP_TO_DATE
    }

    def "Creates JAR file with populated manifest attributes by basic info plugin"() {
        given:
        writeHelloWorld('nebula.test')
        buildFile << """
            plugins {
                id 'com.netflix.nebula.info-broker'
                id 'com.netflix.nebula.info-basic'
                id 'com.netflix.nebula.info-jar'
            }

            apply plugin: 'java'

            version = '1.0'
        """.stripIndent()

        when:
        runTasks('jar')

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
        assertMainfestKeyExists(attributes, 'Build-Date-UTC')
        assertMainfestKeyExists(attributes, 'Build-Timezone')
        assertMainfestKeyExists(attributes, 'Gradle-Version')
        manifestKey(attributes, 'Build-Timezone') == TimeZone.default.getID()
        //Make sure we have ISO-8601 date
        OffsetDateTime.parse(manifestKey(attributes, 'Build-Date-UTC').toString())
    }

    def "Creates JAR file with populated manifest excluding properties in infoBroker configuration"() {
        given:
        writeHelloWorld('nebula.test')
        buildFile << """
           plugins {
                id 'com.netflix.nebula.info-broker'
                id 'com.netflix.nebula.info-basic'
                id 'com.netflix.nebula.info-jar'
            }

            apply plugin: 'java'

            version = '1.0'

            infoBroker {
                excludedManifestProperties = ['Build-Date', 'Built-OS']
            }
        """.stripIndent()

        when:
        runTasks('jar')

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

    def "Creates JAR file with populated manifest including only properties in infoBroker configuration"() {
        given:
        writeHelloWorld('nebula.test')
        buildFile << """
           plugins {
                id 'com.netflix.nebula.info-broker'
                id 'com.netflix.nebula.info-basic'
                id 'com.netflix.nebula.info-jar'
            }

            apply plugin: 'java'

            version = '1.0'

            infoBroker {
                includedManifestProperties = ['Build-Date', 'Built-OS']
            }
        """.stripIndent()

        when:
        runTasks('jar')

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

    def "Creates JAR fails if excludedManifestProperties and excludedManifestProperties are both provided"() {
        given:
        writeHelloWorld('nebula.test')
        buildFile << """
           plugins {
                id 'com.netflix.nebula.info-broker'
                id 'com.netflix.nebula.info-basic'
                id 'com.netflix.nebula.info-jar'
            }

            apply plugin: 'java'

            version = '1.0'

            infoBroker {
                includedManifestProperties = ['Build-Date', 'Built-OS']
                excludedManifestProperties = ['Build-Date', 'Built-OS']
            }
        """.stripIndent()

        when:
        def executionResult = runTasksAndFail('jar')

        then:
        executionResult.output.contains('includedManifestProperties and excludedManifestProperties are mutually exclusive. Only one should be provided')
    }


    def "changes to group and version are reflected"() {
        given:
        writeHelloWorld('nebula.test')
        buildFile << """
           plugins {
                id 'com.netflix.nebula.info-broker'
                id 'com.netflix.nebula.info-basic'
                id 'com.netflix.nebula.info-jar'
            }

            apply plugin: 'java'

            group = 'com.netflix'
            version = '1.0'
        """.stripIndent()

        when:
        runTasks('jar')

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
            plugins {
                id 'com.netflix.nebula.info-broker'
                id 'com.netflix.nebula.info-basic'
                id 'com.netflix.nebula.info-jar'
            }
        

            apply plugin: 'java'

            afterEvaluate {
                group = 'com.netflix'
                version = '1.0'
            }
        """.stripIndent()

        when:
        runTasks('jar')

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
