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
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Take the contacts and apply them to the manifest, specifically "owners" and "notify". In the presence of the
 * gradle-info-plugin, we'll publish to tags, Module-Owner and Module-Email. Module-Owner is a common separated
 * list of all contacts that have the "owner" role (or no role). Module-Email is a common separated list of
 * contacts that want to be notified when changes are made to this module, they have to have the role of "notify"
 * (or no role). We're assuming that multiple owners are allowed, and we can just comma separate them.
 */
class ManifestOwnersPlugin implements Plugin<Project> {
    public static final String OWNER_ROLE = 'owner'
    public static final String NOTIFY_ROLE = 'notify'

    @Override
    void apply(Project project) {
        // React to BaseContactsPlugin
        project.plugins.withType(BaseContactsPlugin) { BaseContactsPlugin contactsPlugin ->
            // React to Info Plugin
            project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin basePlugin ->

                basePlugin.add('Module-Owner') {
                    contactsPlugin.getContacts(OWNER_ROLE).collect { it.email }.join(',')
                }

                basePlugin.add('Module-Email') {
                    contactsPlugin.getContacts(NOTIFY_ROLE).collect { it.email }.join(',')
                }
            }
        }
    }
}
