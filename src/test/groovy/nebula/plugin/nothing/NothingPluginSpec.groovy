package nebula.plugin.nothing

import nebula.test.ProjectSpec

class NothingPluginSpec extends ProjectSpec {
    def 'apply plugin'() {
        when:
        project.plugins.apply(NothingPlugin)

        then:
        noExceptionThrown()
    }

}