package nebula.plugin.info.scm

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class PerforceScmProviderSpec extends Specification {
    @Rule TemporaryFolder temp
    def provider = new PerforceScmProvider()

    def 'lookup settings'() {
        setup:
        def orig = [P4CONFIG: 'p4config', P4USER: 'jryan']
        def dest = [P4USER: 'aeinstein']

        when:
        def result = provider.overrideFromMap(orig, dest)

        then:
        result.P4CONFIG == 'p4config'
        result.P4USER == 'aeinstein'
    }

    def 'find file in project direct'() {
        setup:
        def projectDir = temp.newFolder()
        def deep = new File(projectDir, "level1/level2/level3")
        deep.mkdirs()
        def configName = 'p4config'
        def config = new File(projectDir, configName)
        config.text = "something"

        when:
        def found = provider.findFile(deep, configName)

        then:
        found.exists()
        found == config

        when:
        def notfound = provider.findFile(deep, "fake")

        then:
        notfound == null
    }

    def 'find perforce defaults'() {
        setup:
        def projectDir = temp.newFolder()
        def config = new File(projectDir, 'p4config')
        config.text = "P4CLIENT=jryan_uber\nP4USER=jryan"
        provider.p4configFile = config

        when:
        def foundMap = provider.perforceDefaults(projectDir)

        then:
        foundMap.P4CLIENT == 'jryan_uber'
        foundMap.P4USER == 'jryan'
        foundMap.P4PORT == 'perforce:1666'
    }

    def 'url looks right'() {
        setup:
        def defaults = [P4USER: 'user', P4PORT: 'port']

        when:
        def result = provider.getUrl(defaults)

        then:
        result == 'p4java://port?userName=user'
    }
}
