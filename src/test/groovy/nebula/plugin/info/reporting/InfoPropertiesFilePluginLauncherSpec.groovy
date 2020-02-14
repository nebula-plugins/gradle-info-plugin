package nebula.plugin.info.reporting

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.basic.BasicInfoPlugin
import nebula.test.IntegrationSpec

import java.util.jar.JarFile


class InfoPropertiesFilePluginLauncherSpec extends IntegrationSpec {
    def 'jar task is marked UP-TO-DATE if ran before successfully and metadata changes are ignored'() {
        writeHelloWorld('nebula.test')
        buildFile << """
            ${applyPlugin(InfoBrokerPlugin)}
            ${applyPlugin(BasicInfoPlugin)}
            ${applyPlugin(InfoJarPropertiesFilePlugin)}

            apply plugin: 'java'
            status = 'release'
        """.stripIndent()

        when:
        // Make sure we have some history already in place
        def result = runTasksSuccessfully('jar')
        runTasksSuccessfully('clean')

        result = runTasks('jar')

        then:
        File jarFile = new File(projectDir, "build/libs/${moduleName}.jar")
        jarFile.exists()
        Properties metadata = getPropertiesFromJar(jarFile)
        metadata['Built-Status'] == 'release'
        !result.wasUpToDate(':jar')

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
        File reusedJar = new File(projectDir, "build/libs/${moduleName}.jar")
        reusedJar.exists()
        Properties staleMetadata = getPropertiesFromJar(reusedJar)
        //metadata change is ignored and cache is reused
        staleMetadata['Built-Status'] == 'release'
        thirdResult.wasUpToDate(':jar')
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
}
