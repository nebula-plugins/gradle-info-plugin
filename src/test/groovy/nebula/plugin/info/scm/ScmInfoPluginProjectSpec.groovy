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
package nebula.plugin.info.scm

import nebula.test.ProjectSpec

class ScmInfoPluginProjectSpec extends ProjectSpec {
    /**
     * Very fragile, since we're picking up this plugin's git
     */
    def 'apply plugin'() {
        when:
        project.apply plugin: 'com.netflix.nebula.info-scm'

        then:
        def plugin = project.plugins.getPlugin(ScmInfoPlugin)
        plugin != null
        plugin.selectedProvider instanceof GitScmProvider

        def extension = project.extensions.getByType(ScmInfoExtension)
        extension != null
        extension.source.get().startsWith('/build/nebulatest')
        extension.origin.get().contains('plugin')
    }


    def 'apply plugin multi project'() {
        when:
        project.apply plugin: 'com.netflix.nebula.info-scm'
        def subprojectA = addSubproject("a")
        def subprojectB = addSubproject("b")
        subprojectA.apply plugin: 'com.netflix.nebula.info-scm'
        subprojectB.apply plugin: 'com.netflix.nebula.info-scm'

        then:
        def pluginSubProjectA = subprojectA.plugins.getPlugin(ScmInfoPlugin)
        pluginSubProjectA != null
        pluginSubProjectA.selectedProvider instanceof GitScmProvider

        def pluginSubProjectB = subprojectB.plugins.getPlugin(ScmInfoPlugin)
        pluginSubProjectB != null
        pluginSubProjectB.selectedProvider instanceof GitScmProvider

        def extensionSubProjectA = subprojectA.extensions.getByType(ScmInfoExtension)
        extensionSubProjectA != null
        extensionSubProjectA.source.get().startsWith('/build/nebulatest')
        extensionSubProjectA.origin.get().contains('plugin')

        def extensionSubProjectB = subprojectB.extensions.getByType(ScmInfoExtension)
        extensionSubProjectB != null
        extensionSubProjectB.source.get().startsWith('/build/nebulatest')
        extensionSubProjectB.origin.get().contains('plugin')
    }
}
