/*
 * Copyright 2016 Netflix, Inc.
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

import spock.lang.Requires
import spock.lang.Specification

class POSIXUtilSpec extends Specification {
    @Requires({os.linux || os.macOs})
    def 'check getHostName returns a value'() {
        String host = POSIXUtil.hostName

        expect:
        host != null
    }
}
