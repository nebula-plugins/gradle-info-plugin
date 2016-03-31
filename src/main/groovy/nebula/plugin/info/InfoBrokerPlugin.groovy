package nebula.plugin.info

import groovy.transform.Canonical
import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Broker between Collectors and Reporters. Collectors report to this plugin about manifest values,
 * Reporters call this plugin to get values.
 *
 * TBD Allow user build scripts to easily provide values, maybe via an extension
 * TBD Make thread-safe
 */
class InfoBrokerPlugin implements Plugin<Project> {

    private List<ManifestEntry> manifestEntries
    private Map<String, Collection<Closure>> watchers
    private Map<String, Object> reportEntries
    private AtomicBoolean buildFinished = new AtomicBoolean(false)

    void apply(Project project) {

        manifestEntries = new ArrayList<ManifestEntry>()
        reportEntries = new HashMap<>()
        watchers = [:].withDefault { [] }

        project.rootProject.gradle.addBuildListener(new BuildAdapter() {
            @Override
            void buildFinished(BuildResult buildResult) {
                this.buildFinished.set(true)
            }
        })
        // Leaving out for now. I find that the configure methods, when called this way, aren't being called.
        //project.getExtensions().add('manifest', container)

        // The idea is that other plugins will grab a reference to this plugin or manifestEntry and add their entries
    }

    def add(String key, Closure closure) {
        def entry = new ManifestEntry(key, closure)
        addEntry(entry)
    }

    def add(String key, Object value) {
        def entry = new ManifestEntry(key, value)
        addEntry(entry)
    }

    def addReport(String reportName, Object value) {
        if (reportEntries.containsKey(reportName)) {
            def existingEntry = reportEntries.get(reportName)
            throw new IllegalStateException("A report with key $reportName already exists, with the value \"${existingEntry}\"")
        }
        reportEntries.put(reportName, value)
    }

    private ManifestEntry addEntry(ManifestEntry entry) {
        def existing = manifestEntries.find { it.name == entry.name }
        if (existing) {
            resolve(existing)
            throw new IllegalStateException("A entry with the key ${entry.name} already exists, with the value \"${existing.value}\"")
        }
        manifestEntries.add( entry )
        def specificWatchers = watchers[entry.name]
        specificWatchers.each {
            callWatcher(entry, it)
        }
        // Clean up watchers which were called, so that someone could inspect what watchers were no called later.
        specificWatchers.clear()

        return entry
    }

    Map<String, String> buildNonChangingManifest() {
        return collectEntries(manifestEntries.findAll { it.changing == false })
    }

    Map<String, String> buildManifest() {
        return collectEntries(manifestEntries)
    }

    Map<String, Object> buildReports() {
        println "About to check if build finished: ${buildFinished.get()}"

        if (!buildFinished.get()) {
            throw new IllegalStateException('Cannot retrieve build reports before the build has finished')
        }
        return Collections.unmodifiableMap(reportEntries)
    }

    private Map<String,String> collectEntries(Collection<ManifestEntry> entries) {

        // We can't validate via all() because multiple calls would leave the all's closure around
        (Map<String, String>) entries.collectEntries { ManifestEntry entry ->

            resolve(entry)
            return [entry.name, entry.value]
        }.findAll {
            it.value != null
        }.collectEntries {
            [it.key, it.value.toString()]
        }
    }

    /**
     * Resolves entry, forcing a call to the valueProvided if needed, then caching it.
     */
    private void resolve(ManifestEntry entry) {
        // Validate, we can't do this earlier since objects are configured after being added.
        if (!(entry.value || entry.valueProvider)) {
            throw new GradleException("Manifest entry (${entry.name}) is missing a value")
        }

        if (!entry.value && entry.valueProvider) {
            // Force resolution
            def value = entry.valueProvider.call()
            // And then cache value, even nulls
            entry.value = value
        }
    }

    static getPlugin(Project project) {
        return project.getPlugins().getPlugin(InfoBrokerPlugin)
    }

    String buildManifestString() {
        def attrs = buildManifest()
        def manifestStr = attrs.collect { "${it.key}: ${it.value}"}.join('\n      ')
        return manifestStr
    }

    String buildString(String indent = '') {
        def attrs = buildManifest()
        def manifestStr = attrs.collect { "${indent}${it.key}: ${it.value}"}.join('\n')
        return manifestStr
    }

    /**
     * Very limited accessor. It requires that the key value already exists. Best for testing.
     *
     * @param key
     * @return String based value of entry
     */
    String buildEntry(String key) {
        def entry = manifestEntries.find { it.name == key }
        if (!entry) throw new IllegalArgumentException("Unable to find $key")

        resolve(entry)
        return entry.value
    }

    def watch(String key, Closure reaction) {
        // If we have the key already, we can process it now
        def entry = manifestEntries.find { it.name == key }
        if (entry) {
            callWatcher(entry, reaction)
        } else {
            watchers[key] << reaction
        }
    }

    private callWatcher(ManifestEntry entry, Closure reaction) {
        resolve(entry)
        reaction.call(entry.value)
    }

    @Canonical
    static class ManifestEntry {
        ManifestEntry(String name) {
            this.name = name
        }

        ManifestEntry(String name, Closure<String> valueProvider) {
            this.name = name
            this.valueProvider = valueProvider
        }

        ManifestEntry(String name, Object value, boolean changing = false) {
            this.name = name
            this.value = value
            this.changing = changing
        }

        final String name
        Object value // toString() will be called on it, on every buildManifest
        Closure<String> valueProvider
        boolean changing
    }
}
