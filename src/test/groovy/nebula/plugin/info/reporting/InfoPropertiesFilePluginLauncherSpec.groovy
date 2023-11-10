package nebula.plugin.info.reporting

import nebula.plugin.info.BaseIntegrationTestKitSpec
import org.gradle.testkit.runner.TaskOutcome

import java.util.jar.JarFile


class InfoPropertiesFilePluginLauncherSpec extends BaseIntegrationTestKitSpec {
    def 'jar task is marked UP-TO-DATE if ran before successfully and metadata changes are ignored'() {
        writeHelloWorld('nebula.test')
        buildFile << """
            plugins {
                id 'com.netflix.nebula.info-broker'
                id 'com.netflix.nebula.info-basic'
                id 'com.netflix.nebula.info-jar-properties'
            }

            apply plugin: 'java'
            status = 'release'
        """.stripIndent()

        when:
        // Make sure we have some history already in place
        def result = runTasks('jar')
        runTasks('clean')

        result = runTasks('jar')

        then:
        File originalProperties = new File(projectDir, "build/manifest/${moduleName}.properties")
        originalProperties.exists()
        File jarFile = new File(projectDir, "build/libs/${moduleName}.jar")
        jarFile.exists()
        Properties metadata = getPropertiesFromJar(jarFile)
        metadata['Built-Status'] == 'release'
        result.task(':jar').outcome != TaskOutcome.UP_TO_DATE

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
        File reusedJar = new File(projectDir, "build/libs/${moduleName}.jar")
        reusedJar.exists()
        Properties staleMetadata = getPropertiesFromJar(reusedJar)
        //metadata change is ignored and cache is reused
        staleMetadata['Built-Status'] == 'release'
        thirdResult.task(':jar').outcome == TaskOutcome.UP_TO_DATE
    }

    Properties getPropertiesFromJar(File jar) {
        def file = new JarFile(jar)
        def propertiesEntry = file.getEntry("META-INF/${moduleName}.properties")
        def result = new Properties()
        result.load(file.getInputStream(propertiesEntry))
        result.each {
                println it
        }
        return result
    }

    def 'all jar tasks is marked UP-TO-DATE if ran before successfully and metadata changes are ignored'() {
        writeHelloWorld('nebula.test')
        buildFile << """
              plugins {
                id 'com.netflix.nebula.info-broker'
                id 'com.netflix.nebula.info-basic'
                id 'com.netflix.nebula.info-jar-properties'
            }

            apply plugin: 'java'
            status = 'release'

            task sourceJar(type: Jar) {
                archiveAppendix = 'src'
                from('src')
            }
        """.stripIndent()

        when:
        // Make sure we have some history already in place
        runTasks('jar', 'sourceJar')
        runTasks('clean')

        def result = runTasks('jar', 'sourceJar')

        then:
        result.task(':jar').outcome != TaskOutcome.UP_TO_DATE
        result.task(':sourceJar').outcome != TaskOutcome.UP_TO_DATE

        when: 'Nothing has changed'
        def secondResult = runTasks('jar', 'sourceJar')

        then:
        secondResult.task(':jar').outcome == TaskOutcome.UP_TO_DATE
        secondResult.task(':sourceJar').outcome == TaskOutcome.UP_TO_DATE

        when: 'A manifest field was changed'
        buildFile << """
        status = 'integration'
        """.stripIndent()
        def thirdResult = runTasks('jar', 'sourceJar')

        then:
        thirdResult.task(':jar').outcome == TaskOutcome.UP_TO_DATE
        thirdResult.task(':sourceJar').outcome == TaskOutcome.UP_TO_DATE
    }
}
