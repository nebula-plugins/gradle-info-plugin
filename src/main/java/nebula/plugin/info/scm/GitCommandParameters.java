package nebula.plugin.info.scm;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.ValueSourceParameters;

import java.io.File;

interface GitCommandParameters extends ValueSourceParameters {
    Property<File> getRootDir();
}
