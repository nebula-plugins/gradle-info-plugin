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

    private void assertMainfestKeyExists(Attributes attributes, String key) {
        assert attributes.containsKey(new Attributes.Name(key))
    }

    private manifestKey(Attributes attributes, String key) {
        attributes.get(new Attributes.Name(key))
    }
}
