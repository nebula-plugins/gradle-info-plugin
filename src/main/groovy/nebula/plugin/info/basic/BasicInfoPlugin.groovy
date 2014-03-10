package nebula.plugin.info.basic

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoCollectorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Simple provider, for common fields, like build status. Current values:
 *
 * <ul>
 *     <li>Built-Status (project.status)</li>
 *     <li>Implementation-Title (project.group#project.name;project.version)</li>
 *     <li>Implementation-Version (project.version)</li>
 *     <li></li>
 *     <li></li>
 *     <li></li>
 */
class BasicInfoPlugin implements Plugin<Project>, InfoCollectorPlugin {


    void apply(Project project) {

        // All fields are known upfront, so we pump these in immediately.
        project.plugins.withType(InfoBrokerPlugin) { manifestPlugin ->
            manifestPlugin.add('Manifest-Version', '1.0') // Java Standard
            manifestPlugin.add('Built-Status') { project.status } // Could be promoted, so this is the actual status necessarily
            manifestPlugin.add('Implementation-Title', { "${project.group}#${project.name};${project.version}" } ) // !${jarTask.name}(jar)"
            manifestPlugin.add('Implementation-Version') { project.version }
            manifestPlugin.add('Built-By', System.getProperty('user.name'))

            // Makes list of attributes not idempotent, which can throw off "changed" checks
            manifestPlugin.add('Build-Date', new Date().format('yyyy-MM-dd_HH:mm:ss')).changing = true
        }
    }
}
