package nebula.plugin.info.dependencies

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoCollectorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.DefaultVersionComparator

class DependenciesInfoPlugin implements Plugin<Project>, InfoCollectorPlugin {
    def versionComparator = new DefaultVersionComparator().asStringComparator()

    @Override
    void apply(Project project) {
        project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin manifestPlugin ->
            for(Configuration conf: project.configurations) {
                conf.incoming.afterResolve {
                    manifestPlugin.add("Resolved-Dependencies-${it.name.capitalize()}",
                        it.resolutionResult.allComponents.findAll { it.id instanceof ModuleComponentIdentifier }*.moduleVersion
                                .sort(true, { m1, m2 ->
                                    if(m1.group != m2.group)
                                        return m1.group?.compareTo(m2.group) ?: -1
                                    if(m1.name != m2.name)
                                        return m1.name.compareTo(m2.name) // name is required
                                    versionComparator.compare(m1.version, m2.version)
                                })*.toString().join(','))
                }
            }
        }
    }
}
