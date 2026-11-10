/*
 * Copyright 2016-2019 Netflix, Inc.
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
package nebula.plugin.info.dependencies

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoCollectorPlugin
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.MapProperty
import org.gradle.util.GradleVersion

/**
 * Collects dependency observations and registers explicitly selected file reports.
 *
 * <p>Apply {@code com.netflix.nebula.info-dependencies} and the info broker in each
 * participating project. The {@code dependencyInfo} extension is always registered;
 * the report tasks and variant are registered when the broker is present, in either
 * plugin application order. Applying the collector does not resolve configurations.
 *
 * <p>Without configuration cache, the root broker's {@code resolved-dependencies}
 * report aggregates naturally resolved configurations. The legacy
 * {@code nebulaInfoDependencies} extra property is a read-only aggregate on the root
 * and a read-only local view on a participating child. Applying only to a child
 * does not create this property on the root. These semantics differ from the former
 * shared mutable root map; existing readers that mutate or rely on that alias must migrate.
 *
 * <p>With configuration cache, select inputs through {@link DependencyInfoExtension}
 * and consume the {@code writeDependencyInfo} output, or the root-only
 * {@code writeResolvedDependencies} aggregate. Legacy map evaluation fails because
 * resolution callbacks are not replayed on cache reuse. Report tasks are independent
 * of existing assemble, jar, manifest, and properties task dependencies.
 *
 * <p>This collector's explicit reporting path supports Isolated Projects. It does
 * not establish isolation support for other collectors or the full info facade.
 */
class DependenciesInfoPlugin implements Plugin<Project>, InfoCollectorPlugin {
    /**
     * Registers project-local inputs and observes broker application without resolving dependencies.
     *
     * @param project project that owns the collector, extension, and local report task
     */
    @Override
    void apply(Project project) {
        MapProperty<String, String> dependencies = project.objects.mapProperty(String, String).convention([:])
        def services = project.gradle.sharedServices
        List<String> paths = project.path == ':' ? project.allprojects.collect { it.path }.sort() : [project.path]
        Map initialEntries = project.extensions.extraProperties.has('nebulaInfoDependencies') ?
                project.extensions.extraProperties.get('nebulaInfoDependencies') as Map : [:]
        def reports = new DependencyInfoReports(project.providers.provider(new DependencyInfoReports.Sources(services, paths)), initialEntries)
        def extension = project.extensions.create('dependencyInfo', DependencyInfoExtension)
        boolean cacheRequested = GradleVersion.current() >= GradleVersion.version('8.5') ?
                project.objects.newInstance(DependencyInfoBuildFeatures).configurationCacheRequested :
                project.gradle.startParameter.isConfigurationCache()
        if (!cacheRequested) {
            project.gradle.projectsEvaluated { reports.captureLegacySources() }
        }
        // The root exposes the aggregate compatibility view; children expose only their own inputs.
        project.extensions.extraProperties.set('nebulaInfoDependencies', project.path == ':' ? reports : [:])
        project.plugins.withType(InfoBrokerPlugin) { InfoBrokerPlugin manifestPlugin ->
            String projectName = project.name
            services.registerIfAbsent(DependencyInfoReports.SERVICE_PREFIX + project.path, DependencyInfoState) {
                parameters.projectName.set(projectName)
                parameters.dependencies.set(dependencies)
                parameters.configurationCacheRequested.set(cacheRequested)
            }
            processProjectDependencies(project, dependencies)
            def report = project.tasks.register('writeDependencyInfo', DependencyInfoReportTask) {
                it.group = 'reporting'
                it.description = 'Writes the dependency information explicitly selected in this project.'
                it.projectName.set(projectName)
                it.dependencies.set(extension.dependencies)
                it.explicitSelection.set(extension.explicitSelection)
                it.outputFile.set(project.layout.buildDirectory.file('reports/dependency-info/dependencies.json'))
            }
            def elements = project.configurations.create('nebulaDependencyInfoElements') {
                it.canBeResolved = false
                it.canBeConsumed = true
                it.attributes.attribute(Attribute.of('nebula.dependency-info', String), 'report')
            }
            project.artifacts.add(elements.name, report.flatMap { it.outputFile }) { it.builtBy(report) }
            if (project.path == ':') {
                manifestPlugin.addReport('resolved-dependencies', reports)
                def incoming = project.configurations.create('nebulaDependencyInfoReports') {
                    it.canBeResolved = true
                    it.canBeConsumed = false
                }
                paths.findAll { it != ':' }.each { path ->
                    project.dependencies.add(incoming.name, project.dependencies.project(path: path, configuration: elements.name))
                }
                def files = incoming.incoming.artifactView { it.lenient = true }.files
                def failures = project.providers.provider(new DependencyInfoReportFailures(incoming.incoming.resolutionResult.rootComponent, services))
                project.tasks.register('writeResolvedDependencies', ResolvedDependenciesReportTask) {
                    it.group = 'reporting'
                    it.description = 'Aggregates dependency information from participating projects.'
                    it.reports.from(report.flatMap { it.outputFile }, files)
                    it.resolutionFailures.set(failures)
                    it.outputFile.set(project.layout.buildDirectory.file('reports/dependency-info/resolved-dependencies.json'))
                }
            } else {
                project.extensions.extraProperties.set('nebulaInfoDependencies',
                        reports)
            }
        }
    }

    /** Tracks configuration identities so callbacks from removed or replaced instances are ignored. */
    private static void processProjectDependencies(Project project, MapProperty<String, String> dependencies) {
        Map<String, Long> activeConfigurations = Collections.synchronizedMap(new HashMap<String, Long>())
        def sequence = new java.util.concurrent.atomic.AtomicLong()
        project.configurations.whenObjectRemoved { Configuration conf -> activeConfigurations.remove(conf.name) }
        project.configurations.all({ Configuration conf ->
            long identity = sequence.incrementAndGet()
            activeConfigurations.put(conf.name, identity)
            if (DependenciesInfoSupport.canBeReported(conf)) {
                conf.incoming.afterResolve(new CollectDependencies(dependencies, activeConfigurations, identity))
            }
        })
    }
    private static String resolvedModules(ResolvableDependencies resolvableDependencies) {
        DependenciesInfoSupport.formatModules(resolvableDependencies.resolutionResult.allComponents.findAll {
            it.id instanceof ModuleComponentIdentifier
        }*.moduleVersion)
    }

    /** Records completed resolutions without retaining a Project or Configuration in callback state. */
    private static class CollectDependencies implements Action<ResolvableDependencies> {
        private final MapProperty<String, String> dependencies
        private final Map<String, Long> activeConfigurations
        private final long identity

        CollectDependencies(MapProperty<String, String> dependencies, Map<String, Long> activeConfigurations, long identity) {
            this.dependencies = dependencies
            this.activeConfigurations = activeConfigurations
            this.identity = identity
        }

        @Override
        void execute(ResolvableDependencies incoming) {
            if (activeConfigurations.get(incoming.name) == identity) {
                String modules = resolvedModules(incoming)
                if (modules) {
                    synchronized (dependencies) {
                        dependencies.put("Resolved-Dependencies-${incoming.name.capitalize()}".toString(), modules)
                    }
                }
            }
        }
    }
}
