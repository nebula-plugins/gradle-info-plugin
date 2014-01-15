package nebula.plugin.nothing

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Restores status of project after Java plugin runs. The one caveat is that this plugin has to be run before the
 * BasePlugin is applied, else we can't restore the status.
 */
class NothingPlugin implements Plugin<Project> {

    Logger logger = Logging.getLogger(NothingPlugin);

    Project project

    void apply(Project project) {

        this.project = project

    }
}
