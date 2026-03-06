package nebula.plugin.info

import java.io.File
import java.util.jar.Attributes
import java.util.jar.JarFile

fun readJarAttributes(projectDir: File, moduleName: String, version: String): Attributes {
    return JarFile(projectDir.resolve("build/libs/${moduleName}-${version}.jar")).manifest.mainAttributes
}

fun Attributes.getKey(key: String): Any? {
    return get(Attributes.Name(key))
}
