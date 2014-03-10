package nebula.plugin.info.ci

import org.gradle.api.Project

class UnknownContinuousIntegrationProvider extends AbstractContinuousIntegrationProvider {

    public static final String LOCAL = 'LOCAL'
    @Override
    boolean supports(Project project) {
        return true
    }

    @Override
    String calculateBuildNumber(Project project) {
        return LOCAL
    }

    @Override
    String calculateBuildId(Project project) {
        return LOCAL
    }

    @Override
    String calculateHost(Project project) {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName()
        } catch(UnknownHostException e) {
            return InetAddress.localHost.hostAddress
        }
    }

    @Override
    String calculateJob(Project project) {
        return LOCAL
    }
}
