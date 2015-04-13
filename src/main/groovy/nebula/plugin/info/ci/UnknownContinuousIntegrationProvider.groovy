package nebula.plugin.info.ci

import groovy.util.logging.Log
import org.gradle.api.Project

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
        try {
            return InetAddress.getLocalHost().getCanonicalHostName()
        } catch(UnknownHostException e) {
            try {
                return InetAddress.localHost.hostAddress
            } catch(UnknownHostException e2) {
                log.warning("Your hostname isn't set.")
                // Grab first up and not loopback interface, with an IP address
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces()
                while( interfaces.hasMoreElements() ) {
                    NetworkInterface nif = interfaces.nextElement()
                    if (!nif.loopback && nif.up) {
                        Enumeration<InetAddress> addresses = nif.inetAddresses
                        if( addresses.hasMoreElements()) {
                            return addresses.nextElement().hostAddress
                        }
                    }
                }

                return "localhost"
            }
        }
    }

    @Override
    String calculateJob(Project project) {
        return LOCAL
    }
}
