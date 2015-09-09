package nebula.plugin.info.scm

import nebula.test.ProjectSpec

class ScmInfoPluginProjectSpec extends ProjectSpec {
    /**
     * Very fragile, since we're picking up this plugin's git
     */
    def 'apply plugin'() {
        when:
        project.apply plugin: 'nebula.info-scm'

        then:
        def plugin = project.plugins.getPlugin(ScmInfoPlugin)
        plugin != null
        plugin.selectedProvider instanceof GitScmProvider

        def extension = project.extensions.getByType(ScmInfoExtension)
        extension != null
        extension.source.startsWith('/build/test')
        extension.origin.endsWith('plugin.git')
    }
}
