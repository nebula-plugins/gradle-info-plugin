package nebula.plugin.info.scm

import org.gradle.api.Project


abstract class AbstractScmProvider implements ScmInfoProvider {
    abstract calculateModuleSource(File projectDir)

    @Override
    String calculateSource(Project project) {
        return calculateModuleSource(project.projectDir)
    }

    protected File findFile(File starting, String filename) {
        // TODO Stop looking when we get to the home directory, to avoid paths which we know aren't a SCM root
        if (!filename) {
            return null
        }

        def dirToLookIn = starting
        while(dirToLookIn) {
            def p4configFile = new File(dirToLookIn, filename)
            if (p4configFile.exists()) {
                return p4configFile
            }
            dirToLookIn = dirToLookIn?.getParentFile()
        }
        return null
    }

    @Override
    String calculateOrigin(Project project) {
        return calculateModuleOrigin(project.projectDir)
    }

    abstract calculateModuleOrigin(File projectDir)

    @Override
    String calculateChange(Project project) {
        return calculateChange(project.projectDir)
    }

    abstract calculateChange(File projectDir)

    @Override
    String calculateBranch(Project project) {
        return calculateBranch(project.projectDir)
    }

    abstract calculateBranch(File projectDir)
}
