package nebula.plugin.info.dependencies

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class ResolvedDependenciesReportTask extends DefaultTask {
    @InputFiles @PathSensitive(PathSensitivity.ABSOLUTE)
    abstract ConfigurableFileCollection getReports()
    @Input abstract ListProperty<String> getResolutionFailures()
    @OutputFile abstract RegularFileProperty getOutputFile()

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
