# Dependency report verification

Run from the repository root with a JDK 17 installation:

```sh
./gradlew -p verification/dependency-info test validatePlugins \
  -DdependencyInfo.testJavaHome="$JAVA_HOME" --no-configuration-cache
```

This build uses only core Gradle plugins, the collector/broker source files, and
Spock/TestKit. It does not apply `nebula-plugin-plugin`, the info facade, release,
locking, or publishing plugins. Test dependencies come from per-test local Maven
repositories; fixture builds do not use external repositories or real publishing.

The default consumer matrix is Gradle 7.5.1, 9.2.1 (the repository wrapper), and
9.7.1. Override `-DdependencyInfo.gradleVersions=8.14.3` to check another version.
The test JVM can differ from the consumer daemon JVM: `dependencyInfo.testJavaHome`
selects a compatible JDK for the consumer builds. Use JDK 17 for the complete
matrix; Gradle 7.5.1 cannot run on JDK 21. The compiled standalone plugin targets
Java 17. No minimum consumer Gradle version was previously declared; these checks
establish a tested floor for the collector/report path only.

Tests compare actual JSON contents in no-cache builds, cold cache storage, and
warm reuse after removing generated reports. Strict cache diagnostics are used
throughout. Isolated Projects cases run only on versions that provide the feature;
the Gradle 7.5.1 cases exercise ordinary configuration cache. The matrix covers
root-only, child-only, sibling and nested participants, an empty root, late broker
application, unrelated Java projects, parallel configuration, unused tasks and
configurations, transitive module ordering, filtering, and child dependency changes
followed by warm reuse. Additional tests check detached/replaced configurations,
legacy aggregate/extra-property reads, preexisting report entries, and actionable
errors for unsupported cached legacy readers and unresolved selected dependencies.
Broken participant variants also fail aggregation; lenient resolution only permits
missing report variants on non-participants. Error-handling tests are not
compatibility passes for the rejected operations.

For the existing collector, broker, manifest, properties and facade tests, plus
plugin/ABI validation, use the regular build:

```sh
./gradlew test --tests 'nebula.plugin.info.dependency.*' \
  --tests 'nebula.plugin.info.InfoBroker*' \
  --tests 'nebula.plugin.info.reporting.*' \
  --tests 'nebula.plugin.info.InfoPluginIntegrationSpec' \
  --tests 'nebula.plugin.info.InfoPluginIntegrationTestKitSpec' \
  validatePlugins checkLegacyAbi --no-configuration-cache --continue
```

`--continue` keeps independent checks running after failures; it does not disable
checks or make a failing build successful. In the inspected baseline, the regular
build's `verifyDependencyResolution` check fails resolving existing locked
dependencies, including contacts, interop, Perforce, JNA, and JUnit artifacts. This
also failed before the collector changes. Keep that failure visible and address it
separately; the standalone build isolates report behavior from that blocker.
`validatePlugins` passes. `checkLegacyAbi` was skipped and no tracked ABI baseline
exists, so its invocation is not an ABI compatibility pass.
Public signatures of the collector and broker were compared with the published
16.3.0 classes: existing signatures remain, while the extra-property and cached
legacy-reader behavior changes are documented in the main README.

The final standalone matrix passed 31 tests on JDK 17, including three intentional
error guards. The regular targeted run passed 50 tests with two existing skips on
JDK 21; its overall build still failed dependency verification as described above.

| Consumer Gradle | Ordinary cache: cold and warm explicit reports | Strict Isolated Projects: cold and warm explicit reports |
| --- | --- | --- |
| 7.5.1 | Passed, JDK 17 | Not assessed; this version predates the public feature |
| 8.14.3 | Passed, JDK 17 | Passed, JDK 17 |
| 9.2.1 | Passed, JDK 17 and 21 | Passed, JDK 17 and 21 |
| 9.7.1 | Passed, JDK 17 and 21 | Passed, JDK 17 and 21 |

The full facade still accesses root SCM layout, extensions and plugin state from
child projects. A separate Git-less root/child facade fixture, without the release
or publishing plugins, failed strict isolation on both 9.2.1 and 9.7.1: 9.2.1
reported five problems (root layout/extensions/plugins access), while 9.7.1 stopped
at the first root layout access in `ScmInfoPlugin`'s provider setup. These failures
are not dependency-report compatibility passes. Legacy `buildFinished` callbacks are incompatible with
configuration cache. Broker manifest closures supplied by other collectors or
consumers can also retain unsupported state. These checks establish support for
the explicit dependency report path, not whole-plugin isolation. Do not add blanket
Isolated Projects compatibility metadata based on this suite.
