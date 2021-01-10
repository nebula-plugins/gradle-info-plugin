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

import com.sun.jna.LastErrorException
import com.sun.jna.Library
import com.sun.jna.Native
import groovy.util.logging.Log
import java.util.logging.Level

@Log
class POSIXUtil {
    private static final C c
    static {
        try {
            c = (C) Native.loadLibrary("c", C.class)
        } catch (UnsatisfiedLinkError err) {
            log.log(Level.WARNING, "Unable to load c library", err)
            c = null
        }
    }

    private static interface C extends Library {
        int gethostname(byte[] name, int size_t) throws LastErrorException;
    }

    static String getHostName() {
        if (c != null) {
            byte[] hostname = new byte[256]
            c.gethostname(hostname, hostname.length)
            return Native.toString(hostname)
        }

        log.log(Level.WARNING, "gethostname not available, falling back to calling hostname")
        Process p = Runtime.getRuntime().exec("hostname")
        return p.inputStream.withReader { it.text.trim() }
    }
}
