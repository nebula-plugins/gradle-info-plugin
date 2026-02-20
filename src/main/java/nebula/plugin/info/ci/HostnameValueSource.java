/*
 * Copyright 2014-2026 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nebula.plugin.info.ci;

import com.sun.jna.platform.win32.Kernel32Util;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.gradle.internal.os.OperatingSystem;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public abstract class HostnameValueSource implements ValueSource<String, ValueSourceParameters.None> {
    private static final Logger log = LoggerFactory.getLogger(HostnameValueSource.class);

    @Override
    @Nullable
    public String obtain() {
        return hostname();
    }

    public static String hostname() {
        OperatingSystem currentOs = OperatingSystem.current();
        if (currentOs.isWindows()) {
            try {
                return Kernel32Util.getComputerName();
            } catch (Throwable t) {
                // with variations in Gradle versions and JVMs, this can sometimes break
                log.debug("Unable to determine the host name on this Windows instance", t);
            }
        } else if (currentOs.isUnix()) {
            try {
                return POSIXUtil.getHostName();
            } catch (Throwable t) {
                log.debug("Unable to determine the host name", t);
            }
        } else {
            log.debug("Unknown operating system $currentOs, could not detect hostname");
        }
        return "localhost";
    }
}
