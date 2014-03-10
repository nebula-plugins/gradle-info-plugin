package nebula.plugin.info

import nebula.plugin.info.basic.BasicInfoPlugin
import nebula.test.ProjectSpec
import org.gradle.api.NamedDomainObjectContainer
import spock.lang.Ignore

class InfoBrokerPluginSpec extends ProjectSpec {
    def 'apply plugin'() {
        when:
        project.apply plugin: 'info-broker'

        then:
        noExceptionThrown()

        project.plugins.getPlugin(InfoBrokerPlugin) != null
    }

    @Ignore("Not valid until we add manifest as an extension")
    def 'extension can be extended in different ways'() {
        when:
        project.apply plugin: 'info-broker'
        def basePlugin = project.plugins.getPlugin(InfoBrokerPlugin)
        project.manifest {
            MyKey { value = 'MyValue' }
        }

        then:
        noExceptionThrown()

        NamedDomainObjectContainer container = project.extensions.getByName('manifest')
        InfoBrokerPlugin.ManifestEntry entry = container.getByName('MyKey')
        entry instanceof InfoBrokerPlugin.ManifestEntry
        entry.value == 'MyValue'
        entry.valueProvider == null

        when:
        project.manifest.add( new InfoBrokerPlugin.ManifestEntry('MyKey2', 'MyValue2') )

        then:
        noExceptionThrown()
        container.getByName('MyKey2') != null
        container.getByName('MyKey') != null // Still around

        when:
        project.manifest.add( new InfoBrokerPlugin.ManifestEntry('MyKey3', {"MyValue" + "3"}) )

        then:
        noExceptionThrown()
        InfoBrokerPlugin.ManifestEntry entry3 = container.getByName('MyKey3')
        entry3.value == null
        entry3.valueProvider instanceof Closure

        when:
        project.manifest.create('MyKey4') // No name

        then:
        noExceptionThrown() // We have to allow this because the raw call will do an add before configuring, validation is done in buildManifest
    }

    def 'build manifest'() {
        when:
        project.apply plugin: InfoBrokerPlugin
        project.apply plugin: BasicInfoPlugin

        then:
        def basePlugin = project.plugins.getPlugin(InfoBrokerPlugin)
        def attrs = basePlugin.buildManifest()
        attrs['Manifest-Version'] == '1.0'

        when:
        basePlugin.add('MyKey', 'MyValue')

        then:
        def attrs2 = basePlugin.buildManifest()
        attrs2['MyKey'] == 'MyValue'

        when:
        basePlugin.add('MyKey2') { 'MyValue2' }

        then:
        def attrs3 = basePlugin.buildManifest()
        attrs3['MyKey2'] == 'MyValue2'
        attrs3['MyKey'] == 'MyValue' // Still around
    }
}
