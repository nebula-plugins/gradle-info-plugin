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

import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoCollectorPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
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

    private static final List<String> supportedLanguages = ['java', 'groovy', 'scala', 'kotlin']

    @Inject
    InfoJavaPlugin(ProviderFactory providerFactory) {
        this.providers = providerFactory
    }

    void apply(Project project) {
        // This can't change, so we can commit it early
        project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin manifestPlugin ->
            String javaRuntimeVersion = providers.systemProperty("java.runtime.version").get()
            String javaVmVendor = providers.systemProperty("java.vm.vendor").get()

            manifestPlugin.add(CREATED_PROPERTY, "$javaRuntimeVersion ($javaVmVendor)")
        }

        // After-evaluating, because we need to give user a chance to effect the extension
        project.afterEvaluate {
            project.plugins.withType(JavaBasePlugin) {
                project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin manifestPlugin ->
                    JavaCompatibility sourceAndTargetCompatibility = findSourceAndTargetCompatibility(project)
                    manifestPlugin.add(TARGET_PROPERTY, sourceAndTargetCompatibility.targetCompatibility)
                    manifestPlugin.add(SOURCE_PROPERTY, sourceAndTargetCompatibility.sourceCompatibility)

                    Provider<JavaLauncher> javaLauncher = getJavaLauncher(project)
                    if (javaLauncher.isPresent()) {
                        manifestPlugin.add(JDK_PROPERTY, javaLauncher.get().metadata.languageVersion.toString())
                    } else {
                        String javaVersionFromSystemProperty = providers.systemProperty("java.version").get()
                        manifestPlugin.add(JDK_PROPERTY, javaVersionFromSystemProperty)
                    }
                }
            }
        }
    }

    @CompileDynamic
    private static JavaCompatibility findSourceAndTargetCompatibility(Project project) {
        JavaPluginExtension javaPluginExtension = project.extensions.getByType(JavaPluginExtension)
        SourceSet mainSourceSet = javaPluginExtension.sourceSets.findByName("main")
        if(!mainSourceSet) {
            return new JavaCompatibility(javaPluginExtension.sourceCompatibility.toString(), javaPluginExtension.targetCompatibility.toString())
        }
        List<String> compileTaskNames = supportedLanguages.collect { mainSourceSet.getCompileTaskName(it)}
        Set<AbstractCompile> compileTasks = project.tasks.withType(AbstractCompile).findAll {
            it.name in compileTaskNames
        }

        if(!compileTasks) {
            return JavaCompatibility.UNKNOWN
        }

        return new JavaCompatibility(
               compileTasks.sourceCompatibility.sort().last(), compileTasks.targetCompatibility.sort().last()
        )
    }

    @Canonical
    private static class JavaCompatibility {
        String sourceCompatibility
        String targetCompatibility
        static UNKNOWN = new JavaCompatibility("unknown", "unknown")
    }

    private Provider<JavaLauncher> getJavaLauncher(Project project) {
        def toolchain = project.getExtensions().getByType(JavaPluginExtension.class).toolchain
        JavaToolchainService service = project.getExtensions().getByType(JavaToolchainService.class)
        Provider<JavaLauncher> launcher = service.launcherFor(toolchain)
        return launcher
    }
}
