package nebula.plugin.info.ci

import com.sun.jna.LastErrorException
import com.sun.jna.Library
import com.sun.jna.Native
import groovy.util.logging.Log
import org.gradle.api.Project

@Log
class UnknownContinuousIntegrationProvider extends AbstractContinuousIntegrationProvider {
    private static final C c = (C) Native.loadLibrary("c", C.class);

    private static interface C extends Library {
        public int gethostname(byte[] name, int size_t) throws LastErrorException;
    }

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
        byte[] hostname = new byte[255];
        c.gethostname(hostname, hostname.length)
        return Native.toString(hostname)
    }

    @Override
    String calculateJob(Project project) {
        return LOCAL
    }
}
