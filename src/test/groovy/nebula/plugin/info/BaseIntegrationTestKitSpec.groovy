package nebula.plugin.info

import nebula.test.IntegrationTestKitSpec

abstract class BaseIntegrationTestKitSpec extends IntegrationTestKitSpec {
    def setup() {
        // Enable configuration cache :)
        new File(projectDir, 'gradle.properties') << '''org.gradle.configuration-cache=true'''.stripIndent()
    }
}
