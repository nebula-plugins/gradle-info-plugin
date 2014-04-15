package nebula.plugin.info.dependencies

import nebula.plugin.info.InfoBrokerPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ComponentMetadataDetails
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.internal.artifacts.DefaultResolvedArtifact
import org.gradle.api.internal.artifacts.DefaultResolvedDependency

/**
 * TODO Make configuration configurable
 * TODO NOt sure we should force the resolve the configuration, or react it.
 */
class DependencyInfoPlugin implements Plugin<Project> {

    String configurationName = 'runtime'
    public static final String DEFAULT_STATUS = 'release'

    def minimum(Configuration configuration) {
        configuration.resolvedConfiguration.firstLevelModuleDependencies.each { ResolvedDependency resolvedDependency ->
            println "Resolved: ${resolvedDependency}"
            //println "Resolved: ${resolvedDependency.moduleArtifacts.iterator().next()}"
        }
        def miniumum = configuration.resolvedConfiguration.resolvedArtifacts.inject(DEFAULT_STATUS) { String minimumStatus, ResolvedArtifact artifact ->

            if (artifact instanceof DefaultResolvedArtifact) {
                def artifactDefault = (DefaultResolvedArtifact) artifact
                //println ((DefaultResolvedArtifact) artifact).status
                println "${artifactDefault.resolvedDependency}"
                println "${artifactDefault.artifactSource}"
                println "${artifactDefault.ownerSource}"
            } else {
                println "Unable to look at status"
            }
            return minimumStatus
        }
        return miniumum
    }
    @Override
    void apply(Project project) {

        project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin broker ->
            project.configurations.matching { it.name == configurationName }.all { Configuration configuration ->
                broker.add('Status-Minimum') {
                    // TODO My worry is that this will be forced too early, put check in afterResolve
                    println configuration.dump()
                    return minimum(configuration)
                }
            }
        }

        // Record status schemes
        // Might be able to collect statuses here. Hmm, unless it's dynamic?
        project.dependencies.components.eachComponent { ComponentMetadataDetails details ->
            println "${details.id}: ${details.statusScheme}" // [integration, milestone, release]
        }

//                    configuration.resolvedConfiguration.firstLevelModuleDependencies.inject(DEFAULT_STATUS) { ResolvedDependency resolved ->
//
//                        resolved.moduleArtifacts.findAll {it instanceof DefaultResolvedArtifact }.each { DefaultResolvedArtifact artifact ->
//                            if (artifact.status) { // status could be null when downloading sources and javadoc in a detached configuration
//                                // Ensure dependencies aren't upside down.
//                                int projectStatusIdx = statusScheme.indexOf(project.status)
//                                int depStatusIdx = statusScheme.indexOf(artifact.status)
//                                if (depStatusIdx < projectStatusIdx ) {
//                                    // TODO Only throw error if we're publishing and only check confs for which we're publishing
//                                    throw new UpsideDownDependencyException(project.status, resolvedDependency.module.id, artifact.status)
//                                }
//                            }
//
//                        }
    }
}
