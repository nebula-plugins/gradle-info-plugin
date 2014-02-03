Gradle Info Plugin
=====================
Non-invasively collect information about the environment, and make information available to other plugins in a statically typed way. When possible lazily calculate info.

```
buildscript {
    repositories { jcenter() }
    dependencies { classpath 'com.netflix.nebula:gradle-info-plugin:1.9.+' }
}
```

scm-info Plugin
---------------
```
apply plugin: 'scm-info'
```

ci-info Plugin
--------------

```
apply plugin: 'ci-info'
```
