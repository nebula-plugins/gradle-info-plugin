package nebula.plugin.info

import nebula.test.IntegrationSpec

abstract class BaseIntegrationSpec extends IntegrationSpec {
    def setup() {
        // Enable configuration cache :)
        new File(projectDir, 'gradle.properties') << '''org.gradle.configuration-cache=true'''.stripIndent()
    }
}
