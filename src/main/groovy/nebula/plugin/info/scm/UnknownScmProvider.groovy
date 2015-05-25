package nebula.plugin.info.scm

import org.gradle.api.Project

class UnknownScmProvider extends AbstractScmProvider {

    public static final String LOCAL = 'LOCAL'

    @Override
    boolean supports(Project project) {
        return true
    }

    @Override
    def calculateModuleOrigin(File projectDir) {
        return LOCAL
    }

    @Override
    def calculateModuleSource(File projectDir) {
        return projectDir.absolutePath
    }

    @Override
    String calculateChange(File projectDir) {
        return null
    }

    @Override
    def calculateBranch(File projectDir) {
        return null
    }
}
