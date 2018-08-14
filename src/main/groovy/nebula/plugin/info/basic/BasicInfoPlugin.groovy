/*
 * Copyright 2014-2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nebula.plugin.info.basic

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoCollectorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

import static java.util.jar.Attributes.Name.*
/**
 * Simple provider, for common fields, like build status. Current values:
 *
 * <ul>
 *     <li>Built-Status (project.status)</li>
 *     <li>Implementation-Title (project.group#project.name;project.version)</li>
 *     <li>Implementation-Version (project.version)</li>
 *     <li>Built-Status (project.status)</li>
 *     <li>Built-By (user.name)</li>
 *     <li>Build-Date</li>
 *     <li>Build-Date-Format</li>
 *     <li>Gradle-Version (project.gradle.gradleVersion)</li>
 * </ul>
 */
class BasicInfoPlugin implements Plugin<Project>, InfoCollectorPlugin {

    // Sample from commons-lang, and hence via Maven
    // Manifest-Version: 1.0
    // Ant-Version: Apache Ant 1.7.0
    // Created-By: 1.5.0_13-119 (Apple Inc.)
    // Package: org.apache.commons.lang
    // Extension-Name: commons-lang
    // Specification-Version: 2.4
    // Specification-Vendor: Apache Software Foundation
    // Specification-Title: Commons Lang
    // Implementation-Version: 2.4
    // Implementation-Vendor: Apache Software Foundation
    // Implementation-Title: Commons Lang
    // Implementation-Vendor-Id: org.apache
    // X-Compile-Source-JDK: 1.3
    // X-Compile-Target-JDK: 1.2

    void apply(Project project) {

        // All fields are known upfront, so we pump these in immediately.
        project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin manifestPlugin ->
            manifestPlugin.add(MANIFEST_VERSION.toString(), '1.0') // Java Standard
            manifestPlugin.add(IMPLEMENTATION_TITLE.toString()) { "${project.group}#${project.name};${project.version}" }.changing = true
            manifestPlugin.add(IMPLEMENTATION_VERSION.toString()) { project.version }
            manifestPlugin.add('Built-Status') { project.status } // Could be promoted, so this is the actual status necessarily
            manifestPlugin.add('Built-By', System.getProperty('user.name'))
            manifestPlugin.add('Built-OS', System.getProperty('os.name'))

            String buildDateFormat = "yyyy-MM-dd'T'HH:mm:ssXXX"
            // Makes list of attributes not idempotent, which can throw off "changed" checks
            manifestPlugin.add('Build-Date', new Date().format(buildDateFormat)).changing = true
            manifestPlugin.add('Build-Date-Format', buildDateFormat)

            manifestPlugin.add('Gradle-Version', { project.gradle.gradleVersion })

            // TODO Include hostname
        }
    }
}
