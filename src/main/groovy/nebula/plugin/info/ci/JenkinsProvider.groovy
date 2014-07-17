package nebula.plugin.info.ci

import org.gradle.api.Project

class JenkinsProvider extends AbstractContinuousIntegrationProvider {

    @Override
    boolean supports(Project project) {
        getEnvironmentVariable('BUILD_NUMBER') && getEnvironmentVariable('JOB_NAME')
    }

    @Override
    String calculateBuildNumber(Project project) {
        getEnvironmentVariable('BUILD_NUMBER')
    }

    @Override
    String calculateBuildId(Project project) {
        getEnvironmentVariable('BUILD_ID')
    }

    @Override
    String calculateHost(Project project) {
        getEnvironmentVariable('JENKINS_URL')
    }

    @Override
    String calculateJob(Project project) {
        getEnvironmentVariable('JOB_NAME')
    }

    private String getEnvironmentVariable(String envKey) {
        System.getenv(envKey)
    }
}
