package nebula.plugin.info.reporting

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.basic.BasicInfoPlugin
import nebula.test.IntegrationSpec

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
        """.stripIndent()

        when:
        def result = runTasksSuccessfully('jar')

        then:
        !result.wasUpToDate(':jarManifest')

        when:
        def secondResult = runTasksSuccessfully('jar')

        then:
        secondResult.wasUpToDate(':jarManifest')
    }

    def "Task of type ApplyManifest throws exception if provided JAR task name does not have expected type"() {
        given:
        writeHelloWorld('nebula.test')
        buildFile << """
            ${applyPlugin(InfoBrokerPlugin)}
            ${applyPlugin(BasicInfoPlugin)}
            ${applyPlugin(InfoJarManifestPlugin)}

            apply plugin: 'java'

            task nonJarTask

            task myTask(type: nebula.plugin.info.reporting.ApplyManifest) {
                jarTaskName = nonJarTask.name
                jarBeingModified = jar.archivePath
            }
        """.stripIndent()

        when:
        def result = runTasksWithFailure('myTask')

        then:
        result.standardOutput.contains(":myTask FAILED")
        result.standardError.contains("The task with the provided name 'nonJarTask' is not of type Jar.")
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

    private void assertMainfestKeyExists(Attributes attributes, String key) {
        assert attributes.containsKey(new Attributes.Name(key))
    }
}
