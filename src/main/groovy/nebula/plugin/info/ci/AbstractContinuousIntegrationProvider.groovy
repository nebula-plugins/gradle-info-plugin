/*
 * Copyright 2014-2019 Netflix, Inc.
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

package nebula.plugin.info.ci

import com.sun.jna.platform.win32.Kernel32Util
import groovy.util.logging.Log
import org.gradle.api.provider.ProviderFactory
import org.gradle.internal.os.OperatingSystem

import java.util.logging.Level

@Log
abstract class AbstractContinuousIntegrationProvider implements ContinuousIntegrationInfoProvider {

    protected String getEnvironmentVariable(String envKey) {
        return System.getenv(envKey)
    }

    protected static String hostname() {
        OperatingSystem currentOs = OperatingSystem.current()
        if (currentOs.isWindows()) {
            try {
                return Kernel32Util.getComputerName()
            } catch (Throwable ignored) {
                // with variations in Gradle versions and JVMs, this can sometimes break
                log.log(Level.WARNING, "Unable to determine the host name on this Windows instance")
                return 'localhost'
            }
        } else if (currentOs.isUnix()) {
            return POSIXUtil.getHostName()
        } else {
            log.log(Level.WARNING, "Unknown operating system $currentOs, could not detect hostname")
            return 'localhost'
        }
    }
}
