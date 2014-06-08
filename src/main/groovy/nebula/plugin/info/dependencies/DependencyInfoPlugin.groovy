package nebula.plugin.info.dependencies

import nebula.plugin.info.InfoBrokerPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Method

/**
 * TODO Make configuration configurable via DSL
 */
class DependencyInfoPlugin implements Plugin<Project> {

    Logger logger = LoggerFactory.getLogger(DependencyInfoPlugin.class)

    String configurationName = 'runtime'

    // TODO Eliminate need for a default status, we have the scheme and in this case we just want the largest one.
    public static final String DEFAULT_STATUS = 'release'

    def minimum(Configuration configuration) {
        def minimum = configuration.resolvedConfiguration.firstLevelModuleDependencies.inject(DEFAULT_STATUS) { String minimumStatus, ResolvedDependency resolvedDependency ->
            def minimumIdx = resolvedDependency.getStatusScheme().indexOf(minimumStatus)
            def status = resolvedDependency.getStatus()
            def statusIdx = resolvedDependency.getStatusScheme().indexOf(status)
            if (statusIdx < minimumIdx) {
                logger.debug("For ${resolvedDependency.module.id}, chosing ${status} over ${minimumStatus}")
                return status
            } else {
                return minimumStatus
            }
        }
        return minimum
    }

    @Override
    void apply(Project project) {
        // A custom distribution is required to read the status of modules after resolution. Try to protect this plugin
        // against it's use outside the custom distribution
        boolean supportsStatus = ResolvedDependency.class.declaredMethods.any { Method m -> m.getName().equals("getStatusScheme") }

        if(supportsStatus) {
            project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin broker ->
                project.configurations.matching { it.name == configurationName }.all { Configuration configuration ->
                    broker.add('Status-Minimum') {
                        return minimum(configuration)
                    }
                }
            }
        }
    }
}
