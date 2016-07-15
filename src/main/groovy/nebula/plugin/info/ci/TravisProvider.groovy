package nebula.plugin.info.ci

import org.gradle.api.Project

class TravisProvider extends AbstractContinuousIntegrationProvider {
    @Override
    boolean supports(Project project) {
        getEnvironmentVariable('TRAVIS')
    }

    @Override
    String calculateHost(Project project) {
        return hostname()
    }

    @Override
    String calculateJob(Project project) {
        getEnvironmentVariable("TRAVIS_REPO_SLUG")
    }

    @Override
    String calculateBuildNumber(Project project) {
        getEnvironmentVariable("TRAVIS_BUILD_NUMBER")
    }

    @Override
    String calculateBuildId(Project project) {
        getEnvironmentVariable("TRAVIS_BUILD_ID")
    }
}
