/*
 * Copyright 2014-2016 Netflix, Inc.
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
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem

import java.util.logging.Level

@Log
class UnknownContinuousIntegrationProvider extends AbstractContinuousIntegrationProvider {
    public static final String LOCAL = 'LOCAL'

    @Override
    boolean supports(Project project) {
        return true
    }

    @Override
    String calculateBuildNumber(Project project) {
        return LOCAL
    }

    @Override
    String calculateBuildId(Project project) {
        return LOCAL
    }

    @Override
    String calculateHost(Project project) {
        def currentOs = OperatingSystem.current()
        if (currentOs.isWindows()) {
            try {
                return Kernel32Util.getComputerName()
            } catch(Throwable e) {
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

    @Override
    String calculateJob(Project project) {
        return LOCAL
    }
}
