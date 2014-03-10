package nebula.plugin.info.ci

import org.gradle.api.Project

class JenkinsProvider extends AbstractContinuousIntegrationProvider {

    @Override
    boolean supports(Project project) {
        return System.getenv('BUILD_NUMBER') && System.getenv('JOB_NAME')
    }

    @Override
    String calculateBuildNumber(Project project) {
        return System.getenv('BUILD_NUMBER')
    }

    @Override
    String calculateBuildId(Project project) {
        return System.getenv('BUILD_ID')
    }

    @Override
    String calculateHost(Project project) {
        return System.getenv('JENKINS_URL')
    }

    @Override
    String calculateJob(Project project) {
        return System.getenv('JOB_NAME')
    }
}
