Gradle Info Plugin
=====================
Noninvasively collect information about the environment, and make information available to other plugins in a statically typed way. When possible lazily calculate info.

```
buildscript {
    repositories { jcenter() }
    dependencies { classpath 'com.netflix.nebula:gradle-info-plugin:1.9.+' }
}
```

The module is made of three types of plugins.
* Collecting plugins, e.g. scm-info and ci-info. Both of those examples use specific implementations to detect the current
  environment and report values to the broker.
* Reporting plugins, e.g. Jar manifests, a generated file. These have specific formatting and outputting requirements, so they pull
  from the Broker. These will force values into
* A Broker plugin, to collect values from Collecting plugins and make them available to reporting plugins

Both the Collectors and Reporters know about the broker.

info-broker Plugin (Broker)
---------------
```
apply plugin: 'info-broker'
```

This would leave an empty broker around for other plugins to play with.

info-jar Plugin (Reporter)
---------------
```
apply plugin: 'info-jar'
```

Pumps all values from the broker into the manifest of all jar's being built.

info-props Plugin (Reporter)
---------------
```
apply plugin: 'info-props'
```

Creates a property files with broker's values, defaults to "manifest/${baseConvention.archivesBaseName}.properties". Uses
InfoPropertiesFile task to create file.

info-jar-props Plugin (Reporter)
---------------
```
apply plugin: 'info-jar-props'
```

Leverages info-props to create a file, which this plugin then puts into the META-INF of all jars.

info-basic Plugin (Collector)
--------------

```
apply plugin: 'info-basic'
```

Provides some basic values relavant to the Gradle build, e.g. build status.

info-java Plugin (Collector)
--------------

```
apply plugin: 'info-java'
```

Reports on the version of Java being used, and compatibility version if the Java plugin is being used.

info-ci Plugin (Collector)
--------------

```
apply plugin: 'info-ci'
```

Detects the current Continuous Integration environment and reports upon Job Name and Build Number. Currently only Jenkins
is supported.

info-scm Plugin (Collector)
--------------
```
apply plugin: 'info-scm'
```

Detects the current source control being used and reports upon the repository and where in the source the project is.
Git and Perforce are currently supported. Since real java libraries are used to determine these values but we don't want
to pollute too many people with Perforce, all the plugin implementations are optional. You'll have to add a dependency to
your specific SCM implementation. Technically, these are not optional right now, but they will be in the future.

```
buildscript {
    repositories { jcenter() }
    dependencies { classpath 'com.netflix.nebula:gradle-info-plugin:1.9.+' }

    dependencies { classpath 'com.perforce:p4java:2012.3.551082' }
    // or
    dependencies { classpath 'org.eclipse.jgit:org.eclipse.jgit:3.2.0.201312181205-r' }
}
```


info Plugin
--------------
```
apply plugin: 'info'
```

*Uber plugin that applies all other plugins. Since each one is relatively safe to run, this is the recommended plugin to
apply.*

# TODO
* Have release plugin contribute Label
* Link to generated Javadoc