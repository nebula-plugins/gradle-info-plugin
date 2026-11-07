package nebula.plugin.info.dependencies

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*

/**
 * Aggregates participating projects' dependency report artifacts as UTF-8 JSON.
 *
 * <p>Registered as {@code writeResolvedDependencies} only on the root when its
 * collector and broker are applied. Consumes the root's local report and child
 * {@code nebulaDependencyInfoElements} artifacts, which provide task dependencies
 * without reading child project models. Projects without a report variant are
 * skipped only when they are not registered participants. Broken participating
 * variants and other resolution failures must fail aggregation.
 *
 * <p>The default output is
 * {@code build/reports/dependency-info/resolved-dependencies.json}. Report keys use
 * project names, matching the legacy format. Files are merged in absolute-path
 * order and keys are sorted; if inputs contain the same project-name key, the
 * later file replaces the earlier entry. Only explicitly selected configurations
 * appear, regardless of other configurations resolved elsewhere in the build.
 */
@CacheableTask
abstract class ResolvedDependenciesReportTask extends DefaultTask {
    /** @return root and participating child JSON artifacts, including their producer task dependencies */
    @InputFiles @PathSensitive(PathSensitivity.ABSOLUTE)
    abstract ConfigurableFileCollection getReports()
    /** @return collection failures that must be rejected rather than silently omitted */
    @Input abstract ListProperty<String> getResolutionFailures()
    /** @return JSON file containing the merged project reports */
    @OutputFile abstract RegularFileProperty getOutputFile()

    /**
     * Validates artifact collection and merges the selected project reports.
     *
     * @throws GradleException if the report graph contains a non-ignorable resolution failure
     */
    @TaskAction
    void writeReport() {
        if (!resolutionFailures.get().empty) {
            throw new GradleException('Could not collect dependency reports:\n' + resolutionFailures.get().join('\n'))
        }
        Map result = new TreeMap<>()
        reports.files.sort { it.path }.each { file ->
            result.putAll((Map) new JsonSlurper().parse(file, 'UTF-8'))
        }
        outputFile.get().asFile.write(JsonOutput.toJson(result) + '\n', 'UTF-8')
    }
}
