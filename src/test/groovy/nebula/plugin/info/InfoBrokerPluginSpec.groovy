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

package nebula.plugin.info

import nebula.plugin.info.basic.BasicInfoPlugin
import nebula.test.ProjectSpec
import org.gradle.api.NamedDomainObjectContainer
import spock.lang.Ignore

import java.util.concurrent.atomic.AtomicBoolean

class InfoBrokerPluginSpec extends ProjectSpec {
    def 'apply plugin'() {
        when:
        project.apply plugin: 'nebula.info-broker'

        then:
        noExceptionThrown()

        project.plugins.getPlugin(InfoBrokerPlugin) != null
    }

    @Ignore("Not valid until we add manifest as an extension")
    def 'extension can be extended in different ways'() {
        when:
        project.apply plugin: 'nebula.info-broker'
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

    def 'it throws an exception when build reports are requested prior to build end'() {
        given:
        project.apply plugin: InfoBrokerPlugin

        when:
        def infoBrokerPlugin = project.plugins.getPlugin(InfoBrokerPlugin)
        infoBrokerPlugin.addReport('test', 'some value')
        def reports = infoBrokerPlugin.buildReports()

        then:
        thrown IllegalStateException
    }

    def 'can not add multiple values'() {
        when:
        InfoBrokerPlugin broker = project.plugins.apply(InfoBrokerPlugin)
        broker.add('Key', 'Value')

        then:
        noExceptionThrown()

        when:
        broker.add('Key', 'Value2')

        then:
        def thrown = thrown(IllegalStateException)
        thrown.message == 'A entry with the key Key already exists, with the value "Value"'
    }

    def 'listen for values'() {
        when:
        def broker = project.plugins.apply(InfoBrokerPlugin)

        AtomicBoolean watched = new AtomicBoolean(false)
        AtomicBoolean ran = new AtomicBoolean(false)
        String watchedValue
        broker.watch('Manifest-Version') {
            watched.set(true)
            watchedValue = it
        }

        String delayedValue
        broker.watch('Delayed') {
            ran.set(true)
            delayedValue = it
        }

        then:
        ran.get() == false
        watched.get() == false

        when:
        broker.add('Manifest-Version') {
            '1.0'
        }

        then: 'Delayed is not triggered too'
        ran.get() == false
        watched.get() == true
        watchedValue == '1.0'

        when:
        broker.add('Delayed') {
            '2.0'
        }

        then:
        ran.get() == true
        delayedValue == '2.0'

    }
}
