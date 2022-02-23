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

package nebula.plugin.info.basic
import nebula.plugin.contacts.BaseContactsPlugin
import nebula.plugin.info.InfoBrokerPlugin
import nebula.test.PluginProjectSpec
import org.junit.Ignore

class ManifestOwnersPluginSpec extends PluginProjectSpec {
    @Override
    String getPluginName() {
        'nebula.info-owners'
    }

    @Ignore('this now depends on project evaluation, ignore for now')
    def 'values in broker'() {
        def contactsPlugin = project.plugins.apply(BaseContactsPlugin)
        project.plugins.apply(ManifestOwnersPlugin)
        def brokerPlugin = project.plugins.apply(InfoBrokerPlugin)
        contactsPlugin.extension.addPerson('mickey@disney.com') {
            moniker 'Mickey Mouse'
            role 'owner'
        }
        contactsPlugin.extension.addPerson('minnie@disney.com')
        contactsPlugin.extension.addPerson('goofy@disney.com') {
            role 'notify'
        }

        when:
        def manifest = brokerPlugin.buildManifest()

        then:
        manifest['Module-Owner'] == 'mickey@disney.com,minnie@disney.com'
        manifest['Module-Email'] == 'minnie@disney.com,goofy@disney.com'

    }
}
