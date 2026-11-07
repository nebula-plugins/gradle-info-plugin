package nebula.plugin.info.dependency

import groovy.json.JsonSlurper
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import spock.lang.Specification
import spock.lang.TempDir

import java.util.zip.ZipOutputStream

class DependenciesInfoIsolationSpec extends Specification {
    @TempDir File directory

    private static final List<String> VERSIONS = System.getProperty('dependencyInfo.gradleVersions', '9.2.1,9.7.1').split(',').toList()
    private static final String PLUGINS = '''
        plugins {
            id 'com.netflix.nebula.info-broker'
            id 'com.netflix.nebula.info-dependencies'
        }
    '''

    def 'reports single sibling and nested participants with Gradle #version and isolated=#isolated'() {
        given:
        fixture([':', ':alpha', ':group:beta'], [':unrelated', ':group'])
        Map expected = [
            'sample-dependencies': expectedDependencies('1.0'),
            'alpha-dependencies': expectedDependencies('1.0'),
            'beta-dependencies': expectedDependencies('1.0')
        ]

        when:
        def baseline = run(version, false, false, 'writeResolvedDependencies')

        then:
        baseline.task(':writeResolvedDependencies').outcome == TaskOutcome.SUCCESS
        report(':') == expected
        !baseline.tasks.any { it.path.startsWith(':unrelated:') }
        !file('alpha/build/untouched').exists()

        when:
        deleteReports()
        def cold = run(version, true, isolated, 'writeResolvedDependencies')

        then:
        cold.output.contains('Configuration cache entry stored')
        cold.task(':writeResolvedDependencies').outcome == TaskOutcome.SUCCESS
        report(':') == expected
        report(':alpha', false) == ['alpha-dependencies': expectedDependencies('1.0')]
        report(':group:beta', false) == ['beta-dependencies': expectedDependencies('1.0')]

        when: 'Outputs must be reproduced even when the configuration phase is skipped'
        deleteReports()
        def warm = run(version, true, isolated, 'writeResolvedDependencies')

        then:
        warm.output.contains('Reusing configuration cache')
        warm.task(':writeResolvedDependencies').outcome == TaskOutcome.SUCCESS
        report(':') == expected

        when: 'A child dependency changes while root configuration stays the same'
        file('alpha/build.gradle').text = file('alpha/build.gradle').text.replace("fixtures:api:1.0", "fixtures:api:2.0")
        def changed = run(version, true, isolated, 'writeResolvedDependencies')

        then:
        changed.output.contains('Configuration cache entry stored')
        changed.task(':alpha:writeDependencyInfo').outcome == TaskOutcome.SUCCESS
        report(':') == expected + ['alpha-dependencies': expectedDependencies('2.0')]

        when:
        deleteReports()
        def reused = run(version, true, isolated, 'writeResolvedDependencies')

        then:
        reused.output.contains('Reusing configuration cache')
        report(':') == expected + ['alpha-dependencies': expectedDependencies('2.0')]

        where:
        [version, isolated] << VERSIONS.collectMany { supportsIsolation(it) ? [[it, false], [it, true]] : [[it, false]] }
    }

    def 'root-only single and child-only application with Gradle #version'() {
        given:
        fixture(participants, others)

        when:
        def baseline = run(version, false, false, task)

        then:
        baseline.task(task).outcome == TaskOutcome.SUCCESS
        report(project, aggregate) == [(key): expectedDependencies('1.0')]

        when:
        deleteReports()
        def cold = run(version, true, supportsIsolation(version), task)

        then:
        cold.output.contains('Configuration cache entry stored')
        report(project, aggregate) == [(key): expectedDependencies('1.0')]

        when:
        deleteReports()
        def warm = run(version, true, supportsIsolation(version), task)

        then:
        warm.output.contains('Reusing configuration cache')
        warm.task(task).outcome == TaskOutcome.SUCCESS
        report(project, aggregate) == [(key): expectedDependencies('1.0')]

        where:
        [version, participants, others, task, project, aggregate, key] << VERSIONS.collectMany { version -> [
            [version, [':'], [], ':writeResolvedDependencies', ':', true, 'sample-dependencies'],
            [version, [':'], [':unrelated'], ':writeResolvedDependencies', ':', true, 'sample-dependencies'],
            [version, [':alpha'], [':', ':unrelated'], ':alpha:writeDependencyInfo', ':alpha', false, 'alpha-dependencies']
        ] }
    }

    def 'applying the child collector does not resolve configurations with Gradle #version'() {
        given:
        fixture([':alpha'], [':'])
        file('alpha/build.gradle') << '''
            configurations.selected.incoming.beforeResolve { throw new GradleException('unexpected resolution') }
        '''

        when:
        def cold = run(version, true, true, ':alpha:help')
        def warm = run(version, true, true, ':alpha:help')

        then:
        cold.task(':alpha:help').outcome == TaskOutcome.SUCCESS
        warm.output.contains('Reusing configuration cache')
        !warm.output.contains('unexpected resolution')

        where:
        version << VERSIONS.findAll { supportsIsolation(it) }
    }

    def 'legacy root reports retain naturally resolved child entries without configuration cache'() {
        given:
        fixture([':', ':alpha', ':group:beta'], [':group', ':unrelated'])
        file('build.gradle') << '''
            def broker = plugins.getPlugin(nebula.plugin.info.InfoBrokerPlugin)
            afterEvaluate { assert nebulaInfoDependencies['sample-dependencies'] == [:] }
            gradle.buildFinished {
                println 'LEGACY-REPORT=' + groovy.json.JsonOutput.toJson(broker.buildReports()['resolved-dependencies'])
                println 'EXTRA-REPORT=' + groovy.json.JsonOutput.toJson(nebulaInfoDependencies)
            }
        '''
        [':', ':alpha', ':group:beta'].each { path ->
            file(projectDirectory(path) + 'build.gradle') << '''
                tasks.register('resolve', Copy) {
                    from configurations.selected
                    into layout.buildDirectory.dir('resolved')
                }
            '''
        }

        when:
        def result = run('9.2.1', false, false, ':resolve', ':alpha:resolve', ':group:beta:resolve')

        then:
        def legacy = new JsonSlurper().parseText(result.output.readLines().find { it.startsWith('LEGACY-REPORT=') }.substring('LEGACY-REPORT='.length()))
        legacy == ['sample-dependencies': expectedDependencies('1.0'), 'alpha-dependencies': expectedDependencies('1.0'), 'beta-dependencies': expectedDependencies('1.0')]
        result.output.contains('EXTRA-REPORT=')
    }

    def 'empty root and late broker application retain all participating projects with Gradle #version'() {
        given:
        fixture([':', ':alpha'], [':unrelated'])
        file('build.gradle').text = PLUGINS + '\ndependencyInfo.reportNone()\n'
        file('alpha/build.gradle').text = file('alpha/build.gradle').text.replace(
                "id 'com.netflix.nebula.info-broker'\n            id 'com.netflix.nebula.info-dependencies'",
                "id 'com.netflix.nebula.info-dependencies'\n            id 'com.netflix.nebula.info-broker'")

        when:
        def cold = run(version, true, true, 'writeResolvedDependencies')
        deleteReports()
        def warm = run(version, true, true, 'writeResolvedDependencies')

        then:
        cold.output.contains('Configuration cache entry stored')
        warm.output.contains('Reusing configuration cache')
        report(':') == ['sample-dependencies': [:], 'alpha-dependencies': expectedDependencies('1.0')]

        where:
        version << VERSIONS.findAll { supportsIsolation(it) }
    }

    def 'removed replaced and detached configurations do not enter legacy reports'() {
        given:
        fixture([':'], [])
        file('build.gradle') << '''
            def removed = configurations.create('removed')
            dependencies.add('removed', 'fixtures:api:1.0')
            configurations.remove(removed)
            configurations.create('removed')
            def detached = configurations.detachedConfiguration(dependencies.create('fixtures:api:1.0'))
            removed.resolve()
            detached.resolve()
            configurations.selected.resolve()
            def report = plugins.getPlugin(nebula.plugin.info.InfoBrokerPlugin).buildReports()['resolved-dependencies']
            assert report['sample-dependencies'].keySet() == ['Resolved-Dependencies-Selected'].toSet()
        '''

        expect:
        run('9.2.1', false, false, 'help').task(':help').outcome == TaskOutcome.SUCCESS
    }

    def 'legacy cached reports fail with adoption guidance instead of incomplete data'() {
        given:
        fixture([':'], [])
        file('build.gradle') << '''
            class ReadLegacyReport extends DefaultTask {
                @Internal Map report
                @TaskAction void readReport() { println groovy.json.JsonOutput.toJson(report) }
            }
            def report = plugins.getPlugin(nebula.plugin.info.InfoBrokerPlugin).buildReports()['resolved-dependencies']
            tasks.register('readLegacy', ReadLegacyReport) { it.report = report }
        '''

        when:
        def result = runner('9.2.1', true, true, 'readLegacy').buildAndFail()

        then:
        result.output.contains('Use the writeResolvedDependencies task output')
        result.output.contains('Resolution callbacks are not replayed')
    }

    def 'unresolved selected dependencies fail the report instead of being omitted'() {
        given:
        fixture([':', ':alpha'], [])
        file('alpha/build.gradle') << "\ndependencies { selected 'missing:selected:99' }\n"

        when:
        def result = runner('9.2.1', true, true, 'writeResolvedDependencies').buildAndFail()

        then:
        result.output.contains("Could not resolve dependencies for configuration 'selected'")
        !file('build/reports/dependency-info/resolved-dependencies.json').exists()
    }

    def 'preexisting extra report entries remain visible without configuration cache'() {
        given:
        fixture([':'], [])
        file('build.gradle').text = file('build.gradle').text.replace(
                "id 'com.netflix.nebula.info-dependencies'",
                "id 'com.netflix.nebula.info-dependencies' apply false")
                .replace('repositories {', """
                    ext.nebulaInfoDependencies = ['custom-dependencies': ['custom': 'value']]
                    apply plugin: 'com.netflix.nebula.info-dependencies'
                    repositories {
                """)
        file('build.gradle') << '''
            configurations.selected.resolve()
            assert nebulaInfoDependencies['custom-dependencies'] == ['custom': 'value']
            assert nebulaInfoDependencies['sample-dependencies']['Resolved-Dependencies-Selected'].contains('fixtures:api:1.0')
        '''

        expect:
        run('9.2.1', false, false, 'help').task(':help').outcome == TaskOutcome.SUCCESS
    }

    def 'broken participant report variants cannot silently disappear from aggregation'() {
        given:
        fixture([':', ':alpha'], [':unrelated'])
        file('alpha/build.gradle') << '\nconfigurations.nebulaDependencyInfoElements.canBeConsumed = false\n'

        when:
        def result = runner('9.2.1', true, true, 'writeResolvedDependencies').buildAndFail()

        then:
        result.output.contains('Could not collect dependency reports')
        result.output.contains('project :alpha')
        !file('build/reports/dependency-info/resolved-dependencies.json').exists()
    }

    private void fixture(List<String> participants, List<String> others) {
        publish('fixtures', 'api', '1.0', '<dependency><groupId>aaa</groupId><artifactId>transitive</artifactId><version>1.0</version></dependency>')
        publish('fixtures', 'api', '2.0', '<dependency><groupId>aaa</groupId><artifactId>transitive</artifactId><version>1.0</version></dependency>')
        publish('aaa', 'transitive', '1.0')
        publish('fixtures', 'z-last', '1.0')
        Set paths = (participants + others).toSet()
        file('settings.gradle').text = "rootProject.name = 'sample'\n" + paths.findAll { it != ':' }.collect { "include '${it}'" }.join('\n')
        paths.each { path ->
            String dir = projectDirectory(path)
            File script = file(dir + 'build.gradle')
            if (!participants.contains(path)) {
                script.text = "plugins { id 'java' }\n"
                return
            }
            script.text = PLUGINS + """
                repositories { maven { url = uri('${file('repo').toURI()}') } }
                configurations.create('selected')
                configurations.create('empty')
                configurations.create('neverResolved')
                configurations.create('filteredCompile')
                configurations.create('filteredRUNTIME')
                configurations.create('filteredCompileOnly')
                dependencies {
                    selected 'fixtures:z-last:1.0'
                    selected 'fixtures:api:1.0'
                    neverResolved 'missing:unused:99'
                    filteredCompile 'missing:filtered:99'
                    filteredRUNTIME 'missing:filtered:99'
                    filteredCompileOnly 'missing:filtered:99'
                }
                dependencyInfo.report(configurations.named('selected'))
                dependencyInfo.report(configurations.named('empty'))
                dependencyInfo.report(configurations.filteredCompile)
                dependencyInfo.report(configurations.filteredRUNTIME)
                dependencyInfo.report(configurations.filteredCompileOnly)
                dependencyInfo.report(configurations.named('filteredCompile'))
                configurations.neverResolved.incoming.beforeResolve { throw new GradleException('unexpected unrelated resolution') }
                tasks.register('untouched') { throw new GradleException('task realized unnecessarily') }
            """
        }
    }

    private void publish(String group, String module, String version, String transitive = '') {
        String path = "repo/${group.replace('.', '/')}/${module}/${version}/${module}-${version}"
        file(path + '.pom').text = """<project><modelVersion>4.0.0</modelVersion><groupId>${group}</groupId><artifactId>${module}</artifactId><version>${version}</version><dependencies>${transitive}</dependencies></project>"""
        new ZipOutputStream(new FileOutputStream(file(path + '.jar'))).close()
    }

    private static Map expectedDependencies(String version) {
        ['Resolved-Dependencies-Selected': "aaa:transitive:1.0,fixtures:api:${version},fixtures:z-last:1.0".toString()]
    }

    private Map report(String path, boolean aggregate = true) {
        new JsonSlurper().parse(file(projectDirectory(path) + 'build/reports/dependency-info/' + (aggregate ? 'resolved-dependencies.json' : 'dependencies.json'))) as Map
    }

    private void deleteReports() {
        directory.eachFileRecurse { if (it.name.endsWith('.json') && it.path.contains('/reports/dependency-info/')) assert it.delete() }
    }

    private def run(String version, boolean cache, boolean isolated, String... tasks) {
        runner(version, cache, isolated, tasks).build()
    }

    private GradleRunner runner(String version, boolean cache, boolean isolated, String... tasks) {
        List args = tasks.toList() + [cache ? '--configuration-cache' : '--no-configuration-cache', '--configuration-cache-problems=fail', '--parallel', '--max-workers=4', '--stacktrace', '--no-build-cache', '-Dorg.gradle.java.home=' + System.getProperty('dependencyInfo.testJavaHome', System.getProperty('java.home'))]
        if (isolated) args += '-Dorg.gradle.unsafe.isolated-projects=true'
        GradleRunner.create().withProjectDir(directory).withPluginClasspath().withGradleVersion(version).withArguments(args)
    }

    private File file(String path) {
        File result = new File(directory, path)
        result.parentFile.mkdirs()
        result
    }

    private static String projectDirectory(String path) {
        path == ':' ? '' : path.substring(1).replace(':', '/') + '/'
    }

    private static boolean supportsIsolation(String version) {
        GradleVersion.version(version) >= GradleVersion.version('8.8')
    }
}
