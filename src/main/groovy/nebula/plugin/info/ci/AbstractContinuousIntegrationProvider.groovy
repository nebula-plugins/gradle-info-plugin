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

    private final ProviderFactory providerFactory

    AbstractContinuousIntegrationProvider(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory
    }

    protected String getEnvironmentVariable(String envKey) {
        return providerFactory.environmentVariable(envKey).getOrElse(null)
    }

    protected static String hostname() {
        OperatingSystem currentOs = OperatingSystem.current()
        if (currentOs.isWindows()) {
            try {
                return Kernel32Util.getComputerName()
            } catch (Throwable t) {
                // with variations in Gradle versions and JVMs, this can sometimes break
                log.log(Level.FINEST, "Unable to determine the host name on this Windows instance", t)
            }
        } else if (currentOs.isUnix()) {
            try {
                return POSIXUtil.getHostName()
            } catch (Throwable t) {
                log.log(Level.FINEST, "Unable to determine the host name", t)
            }
        } else {
            log.log(Level.FINEST, "Unknown operating system $currentOs, could not detect hostname")
        }
        return 'localhost'
    }
}
