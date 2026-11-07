package nebula.plugin.info.dependencies

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Writes this project's explicitly selected module dependencies as UTF-8 JSON.
 *
 * <p>Registered as {@code writeDependencyInfo} when the collector and broker are
 * both applied. Inputs come from {@link DependencyInfoExtension}; configure
 * {@code dependencyInfo.report(...)} or {@code dependencyInfo.reportNone()} in the
 * owning project. The output has the shape
 * {@code {"<project-name>-dependencies":{"Resolved-Dependencies-<Configuration>":"group:name:version,..."}}}.
 * An explicitly empty project still contributes its project key with an empty map.
 *
 * <p>The default output is {@code build/reports/dependency-info/dependencies.json}.
 * Wire this task's output provider into consuming tasks so Gradle tracks ordering
 * and inputs. Unlike the legacy callback map, these declared inputs support
 * configuration-cache reuse; Gradle may resolve them while storing the cache.
 */
@CacheableTask
abstract class DependencyInfoReportTask extends DefaultTask {
    /** @return project name used in the top-level report key */
    @Input abstract Property<String> getProjectName()
    /** @return configuration keys and sorted module notations from explicitly selected inputs */
    @Input abstract MapProperty<String, String> getDependencies()
    /** @return whether report inputs, including none, were explicitly selected */
    @Input abstract Property<Boolean> getExplicitSelection()
    /** @return JSON output file consumed locally or published for root aggregation */
    @OutputFile abstract RegularFileProperty getOutputFile()

    /**
     * Validates explicit selection and writes a deterministic report with a trailing newline.
     *
     * @throws GradleException if neither report inputs nor an explicit empty report were declared
     */
    @TaskAction
    void writeReport() {
        if (!explicitSelection.get()) {
            throw new GradleException('Declare report inputs with dependencyInfo.report(configuration) or dependencyInfo.reportNone()')
        }
        outputFile.get().asFile.write(JsonOutput.toJson([(projectName.get() + '-dependencies'): new TreeMap<>(dependencies.get())]) + '\n', 'UTF-8')
    }
}
