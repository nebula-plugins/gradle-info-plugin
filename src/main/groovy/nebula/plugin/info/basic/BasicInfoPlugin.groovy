/*
 * Copyright 2014-2019 Netflix, Inc.
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
import org.gradle.api.provider.ProviderFactory

import javax.inject.Inject
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference

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
 *     <li>Gradle-Version (project.gradle.gradleVersion)</li>
 * </ul>
 */
class BasicInfoPlugin implements Plugin<Project>, InfoCollectorPlugin {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern('yyyy-MM-dd_HH:mm:ss')
    private static final DateTimeFormatter DATE_TIME_ISO_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME

    public static final String BUILT_BY_PROPERTY = 'Built-By'
    public static final String BUILT_OS_PROPERTY = 'Built-OS'
    public static final String BUILD_DATE_PROPERTY = 'Build-Date'
    public static final String BUILD_DATE_UTC_PROPERTY = 'Build-Date-UTC'
    public static final String BUILD_TIMEZONE_PROPERTY = 'Build-Timezone'
    public static final String BUILD_STATUS_PROPERTY = 'Built-Status'
    public static final String GRADLE_VERSION_PROPERTY = 'Gradle-Version'

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
    private final ProviderFactory providers

    @Inject
    BasicInfoPlugin(ProviderFactory providerFactory) {
        this.providers = providerFactory
    }

    void apply(Project project) {
        // All fields are known upfront, so we pump these in immediately.
        project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin manifestPlugin ->
            manifestPlugin.add(MANIFEST_VERSION.toString(), '1.0') // Java Standard

            def projectName = project.name
            AtomicReference<String> projectGroup = new AtomicReference<>()
            AtomicReference<String> projectVersion = new AtomicReference<>()
            AtomicReference<String> projectStatus = new AtomicReference<>()

            setProjectFields(project, projectGroup, projectVersion, projectStatus)

            project.afterEvaluate {
                setProjectFields(project, projectGroup, projectVersion, projectStatus)
            }

            project.gradle.taskGraph.whenReady {
                setProjectFields(project, projectGroup, projectVersion, projectStatus)
            }

            manifestPlugin.add(IMPLEMENTATION_TITLE.toString(), { "${projectGroup.get()}#${projectName};${projectVersion.get()}" })
            manifestPlugin.add(IMPLEMENTATION_VERSION.toString(), { projectVersion.get() })
            manifestPlugin.add(BUILD_STATUS_PROPERTY, { projectStatus.get() }) // Could be promoted, so this is the actual status necessarily

            String builtBy = providers.systemProperty("user.name").get()
            String builtOs = providers.systemProperty("os.name").get()
            manifestPlugin.add(BUILT_BY_PROPERTY, builtBy)
            manifestPlugin.add(BUILT_OS_PROPERTY, builtOs)

            manifestPlugin.add(BUILD_TIMEZONE_PROPERTY, TimeZone.default.getID())

            // Makes list of attributes not idempotent, which can throw off "changed" checks
            Instant now = Instant.now()
            manifestPlugin.add(BUILD_DATE_UTC_PROPERTY, now.toString()).changing = true

            manifestPlugin.add(BUILD_DATE_PROPERTY, DATE_TIME_FORMATTER.format(now.atZone(TimeZone.default.toZoneId()))).changing = true

            manifestPlugin.add(GRADLE_VERSION_PROPERTY, project.gradle.gradleVersion)

            // TODO Include hostname
        }
    }

    private static void setProjectFields(Project project, AtomicReference<String> projectGroup, AtomicReference<String> projectVersion, AtomicReference<String> projectStatus) {
        projectGroup.set(project.group.toString())
        projectVersion.set(project.version.toString())
        projectStatus.set(project.status.toString())
    }
}
