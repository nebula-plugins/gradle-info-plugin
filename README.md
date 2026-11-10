Gradle Info Plugin
=====================
![Support Status](https://img.shields.io/badge/nebula-active-green.svg)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com.netflix.nebula/gradle-info-plugin/maven-metadata.xml.svg?label=gradlePluginPortal)](https://plugins.gradle.org/plugin/com.netflix.nebula.info)
[![Maven Central](https://img.shields.io/maven-central/v/com.netflix.nebula/gradle-info-plugin)](https://maven-badges.herokuapp.com/maven-central/com.netflix.nebula/gradle-info-plugin)
![Build](https://github.com/nebula-plugins/gradle-info-plugin/actions/workflows/build.yml/badge.svg)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/gradle-info-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)

Noninvasively collect information about the environment, and make information available to other plugins in a statically typed way. When possible lazily calculate info.

The module is made of three types of plugins.
* Collecting plugins, e.g. scm-info and ci-info. Both of those examples use specific implementations to detect the current
  environment and report values to the broker.
* Reporting plugins, e.g. Jar manifests, a generated file. These have specific formatting and outputting requirements, so they pull
  from the Broker. These will force values into
* A Broker plugin, to collect values from Collecting plugins and make them available to reporting plugins

Both the Collectors and Reporters know about the broker.

info Plugin
--------------
```
apply plugin: 'com.netflix.nebula.info'
```

*Uber plugin that applies all other plugins. Since each one is relatively safe to run, this is the recommended plugin to
apply.*

info-broker Plugin (Broker)
---------------
```
apply plugin: 'com.netflix.nebula.info-broker'
```

This would leave an empty broker around for other plugins to play with.

info-jar Plugin (Reporter)
---------------
```
apply plugin: 'com.netflix.nebula.info-jar'

// optionally configure MANIFEST.MF entries
infoBroker {
    excludedManifestProperties = ['Build-Date', 'Built-OS']
    // or, but not both!
    includedManifestProperties = ['Build-Date']
}

```

Pumps all values from the broker into the manifest of all jar's being built.

info-props Plugin (Reporter)
---------------
```
apply plugin: 'com.netflix.nebula.info-props'
```

Creates a property files with broker's values, defaults to "manifest/${baseConvention.archivesBaseName}.properties". Uses
InfoPropertiesFile task to create file.

info-jar-props Plugin (Reporter)
---------------
```
apply plugin: 'com.netflix.nebula.info-jar-props'
```

Leverages info-props to create a file, which this plugin then puts into the META-INF of all jars.

info-basic Plugin (Collector)
--------------

```
apply plugin: 'nebula.info-basic'
```

Provides some basic values relavant to the Gradle build, e.g. build status.

info-java Plugin (Collector)
--------------

```
apply plugin: 'com.netflix.nebula.info-java'
```

Reports on the version of Java being used, and compatibility version if the Java plugin is being used.

info-ci Plugin (Collector)
--------------

```
apply plugin: 'com.netflix.nebula.info-ci'
```

Detects the current Continuous Integration environment and reports upon Job Name and Build Number.
Currently supports CircleCI, Cirrus CI, Drone, GitLab CI, Travis CI, and Jenkins.

info-dependencies Plugin (Collector)
--------------
```
apply plugin: 'com.netflix.nebula.info-dependencies'
```

Reports on selected binary module versions (including transitives) of configurations that are resolved.

The broker must also be applied. Without configuration cache, the root broker's
`buildReports()['resolved-dependencies']` and root `nebulaInfoDependencies` extra
property retain the aggregate of naturally resolved configurations. Each
participant contributes a `<project-name>-dependencies` entry, including an empty
map when it has no resolved modules. Configuration keys remain
`Resolved-Dependencies-<CapitalizedConfigurationName>`. Module coordinates are
sorted by group, name, and Gradle's version comparator. Configurations ending in
`compileOnly`, `compile`, or `runtime` (case insensitive), detached configurations,
and configurations removed from the container are excluded. Empty configurations
do not add a configuration key. The collector contributes reports, not manifest
attributes; existing manifest and properties reporters keep their content and
task dependencies.

For configuration cache or Isolated Projects, declare the report's resolution
inputs in **each participating project's own build script or convention plugin**:

```groovy
plugins {
    id 'java'
    id 'com.netflix.nebula.info-broker'
    id 'com.netflix.nebula.info-dependencies'
}

dependencyInfo.report(configurations.named('compileClasspath'))
dependencyInfo.report(configurations.named('runtimeClasspath'))
// A participant with no report inputs must explicitly use dependencyInfo.reportNone().
```

`writeDependencyInfo` writes that project's selected inputs to
`build/reports/dependency-info/dependencies.json`. When the root applies the broker
and collector, `writeResolvedDependencies` aggregates all participating projects to
`build/reports/dependency-info/resolved-dependencies.json`, using project artifact
dependencies. Root-only, child-only, sibling, and nested participation are
supported; child-only builds use the child's `writeDependencyInfo` task.
Non-participants contribute no entries. Either plugin application order is
supported. No dependency configuration is resolved by applying the collector or
declaring a report input. Resolution is deferred until selected report inputs are
queried, normally by the report tasks, including during cache storage.
Unselected configurations remain unresolved. Report tasks are not added to
`assemble`, `jar`, or other existing task dependencies.

Cached consumers must use these task outputs and wire the output provider into
their task inputs, for example:

```groovy
tasks.register('dependencyReportZip', Zip) {
    from(tasks.named('writeResolvedDependencies').flatMap { it.outputFile })
    destinationDirectory = layout.buildDirectory.dir('reports')
    archiveFileName = 'resolved-dependencies.zip'
}
```

Automatic callback reports and `buildFinished` consumers remain a **no-cache
compatibility API**. Gradle does not replay resolution callbacks on cache reuse;
reading the legacy dependency map with configuration cache now fails with adoption
guidance instead of returning an incomplete report. Explicit file reports have the
same report keys, filtering, and sorting, but their scope is the configurations
declared with `dependencyInfo.report`, rather than whichever configurations other
tasks happened to resolve.

The extra property is now a read-only view. Root reads continue to aggregate
participants; child reads expose that child's information. A child no longer
creates an extra property on an otherwise unconfigured root. Apply the collector
to the root to obtain the root view. Preexisting root report entries are retained
for no-cache readers. Consumers that mutate the old shared map, expect every
child's property to alias the root map, or read it with configuration cache must
adopt project-local inputs and the report task outputs. Existing collector and
broker method signatures are retained, but these behavioral changes make this
update backward incompatible for those consumers. Release it in a new major
version unless the legacy behavior is restored.

The collector/report path is verified on Gradle 7.5.1 with JDK 17 (ordinary
configuration cache), and Gradle 8.14.3, 9.2.1, and 9.7.1 with strict Isolated
Projects. The repository has no declared consumer Gradle minimum; 7.5.1 is the
verified floor for this path, not a claim about older versions or the full facade.
This does not establish Isolated Projects support for `com.netflix.nebula.info`,
SCM/CI collectors, arbitrary broker closures, or every reporter combination. See
[the standalone verification guide](verification/dependency-info/README.md).

info-scm Plugin (Collector)
--------------
```
apply plugin: 'com.netflix.nebula.info-scm'
```

Detects the current source control being used and reports upon the repository and where in the source the project is.
Git and Perforce are currently supported. Since real java libraries are used to determine these values but we don't want
to pollute too many people with Perforce, all the plugin implementations are optional. You'll have to add a dependency to
your specific SCM implementation. Technically, these are not optional right now, but they will be in the future.

```
buildscript {
    repositories { mavenCentral() }
    dependencies { classpath 'com.netflix.nebula:gradle-info-plugin:3.3.+' }

    dependencies { classpath 'com.perforce:p4java:2012.3.551082' }
    // or
    dependencies { classpath 'org.eclipse.jgit:org.eclipse.jgit:3.2.0.201312181205-r' }
}
```

### Experimental isolated SCM collection

Enable project-local SCM collection in the root `gradle.properties`:

```properties
nebula.info.isolatedProjectsSupport=true
```

Apply `com.netflix.nebula.info-scm` in each project that needs SCM metadata, usually
through a project convention plugin. No settings plugin is required. The flag is
false by default, retaining the existing root-provider and root-override inheritance.

With the flag enabled, every project owns its `scminfo` extension and selects its
own Git, Perforce, or local fallback provider. Root `scminfo` overrides no longer
propagate to children. Configure shared overrides locally through conventions:

```groovy
plugins { id 'com.netflix.nebula.info-scm' }
scminfo.origin.set('https://example.org/team/library.git')
```

Repository discovery searches the current project and its logical parents using
Gradle's immutable `IsolatedProject.projectDirectory` view. It supports custom
project directories, nested repositories, Git worktrees, and Perforce `P4CONFIG`
files. It does not configure another project or retain a `Project` in SCM provider
execution state. Perforce config contents and Git commands remain tracked provider
inputs, so changed metadata is reflected with configuration-cache reuse.

The isolated path requires Gradle 8.8 or newer; integration tests verify Gradle
9.0.0 and 9.2.1 with parallel execution, strict configuration-cache checks, and
cold/warm Isolated Projects builds. Enable Gradle's mode separately:

```shell
./gradlew <task-consuming-scminfo> -Dorg.gradle.unsafe.isolated-projects=true \
  --configuration-cache --configuration-cache-problems=fail
```

This establishes support for the SCM collector's provider properties only. It does
not establish support for the full `com.netflix.nebula.info` facade, CI collectors,
arbitrary broker closures, or all reporter combinations. Consumers should wire
`scminfo` providers into declared task inputs, rather than read extensions from
task actions.

info-owners (Collector)
--------------
```
apply plugin: 'com.netflix.nebula.info-owners'
```

Collects "owners" and "notify" users and inject them as 'Module-Owner' and 'Module-Email', respectively. The values come
from the gradle-contacts plugin.

```
apply plugin: 'contacts' 
contacts {
    'mickey@disney.com' {
        moniker 'Mickey Mouse'
       role 'owner'
     }
}
```

The above will make sure the manifest contains a Module-Owner of mickey@disney.com.



