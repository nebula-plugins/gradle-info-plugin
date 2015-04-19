package nebula.plugin.info.ci

import spock.lang.Specification

/**
 * Tests for {@link UnknownContinuousIntegrationProvider}.
 */
class UnknownContinuousIntegrationProviderTest extends Specification {
    def 'calculated hostname matches resolved local host'() {
        given:
        def hostname = InetAddress.getLocalHost().getHostName()

        expect:
        new UnknownContinuousIntegrationProvider().calculateHost(null) == hostname
    }
}
