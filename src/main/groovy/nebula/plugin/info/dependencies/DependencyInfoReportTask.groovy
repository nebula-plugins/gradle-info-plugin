package nebula.plugin.info.dependencies

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class DependencyInfoReportTask extends DefaultTask {
    @Input abstract Property<String> getProjectName()
    @Input abstract MapProperty<String, String> getDependencies()
    @Input abstract Property<Boolean> getExplicitSelection()
    @OutputFile abstract RegularFileProperty getOutputFile()

    @TaskAction
    void writeReport() {
        if (!explicitSelection.get()) {
            throw new GradleException('Declare report inputs with dependencyInfo.report(configuration) or dependencyInfo.reportNone()')
        }
        outputFile.get().asFile.write(JsonOutput.toJson([(projectName.get() + '-dependencies'): new TreeMap<>(dependencies.get())]) + '\n', 'UTF-8')
    }
}
