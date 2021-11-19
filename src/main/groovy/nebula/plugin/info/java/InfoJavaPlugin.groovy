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

package nebula.plugin.info.java

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoCollectorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService

import javax.inject.Inject

/**
 * Collect Java relevant fields.
 */
class InfoJavaPlugin implements Plugin<Project>, InfoCollectorPlugin {

    // Apache Commons has a standard convention for these variables
    // http://commons.apache.org/releases/prepare.html

    static final String CREATED_PROPERTY = 'Created-By' // E.g. Created-By: 1.5.0_13-119 (Apple Inc.)
    static final String JDK_PROPERTY = 'Build-Java-Version'

    static final String SOURCE_PROPERTY = 'X-Compile-Source-JDK'
    static final String TARGET_PROPERTY = 'X-Compile-Target-JDK'

    private final ProviderFactory providers

    @Inject
    InfoJavaPlugin(ProviderFactory providerFactory) {
        this.providers = providerFactory
    }

    void apply(Project project) {
        // This can't change, so we can commit it early
        project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin  manifestPlugin ->
            String javaRuntimeVersion = providers.systemProperty("java.runtime.version").forUseAtConfigurationTime().get()
            String javaVmVendor = providers.systemProperty("java.vm.vendor").forUseAtConfigurationTime().get()

            manifestPlugin.add(CREATED_PROPERTY, "$javaRuntimeVersion ($javaVmVendor)")
        }

        // After-evaluating, because we need to give user a chance to effect the extension
        project.afterEvaluate {
            project.plugins.withType(JavaBasePlugin) {
                JavaPluginExtension javaPluginExtension = project.extensions.getByType(JavaPluginExtension)

                project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin manifestPlugin ->
                    manifestPlugin.add(TARGET_PROPERTY, { javaPluginExtension.targetCompatibility } )
                    manifestPlugin.add(SOURCE_PROPERTY, { javaPluginExtension.sourceCompatibility } )
                    Provider<JavaLauncher> javaLauncher = getJavaLauncher(project)
                    if(javaLauncher.isPresent()) {
                        manifestPlugin.add(JDK_PROPERTY, javaLauncher.get().metadata.languageVersion.toString())
                    } else {
                        String javaVersionFromSystemProperty = providers.systemProperty("java.version").forUseAtConfigurationTime().get()
                        manifestPlugin.add(JDK_PROPERTY, javaVersionFromSystemProperty)
                    }
                }
            }
        }
    }

    private Provider<JavaLauncher> getJavaLauncher(Project project) {
        def toolchain = project.getExtensions().getByType(JavaPluginExtension.class).toolchain
        JavaToolchainService service = project.getExtensions().getByType(JavaToolchainService.class)
        Provider<JavaLauncher> launcher = service.launcherFor(toolchain)
        return launcher
    }
}
