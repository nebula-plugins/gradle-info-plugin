package nebula.plugin.info.ci

import com.sun.jna.LastErrorException
import com.sun.jna.Library
import com.sun.jna.Native

class POSIXUtil {
    private static final C c = (C) Native.loadLibrary("c", C.class);

    private static interface C extends Library {
        public int gethostname(byte[] name, int size_t) throws LastErrorException;
    }

    public static String getHostName() {
        byte[] hostname = new byte[256];
        c.gethostname(hostname, hostname.length)
        return Native.toString(hostname)
    }
}
