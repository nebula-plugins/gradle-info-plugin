package nebula.plugin.info.ci;

import java.util.Collection;
import java.util.LinkedList;
import java.util.ServiceLoader;

public class ContinuousIntegrationInfoProviderResolver {

	private static ServiceLoader<ContinuousIntegrationInfoProvider> continuousIntegrationInfoProviderServiceLoader = ServiceLoader
			.load(ContinuousIntegrationInfoProvider.class);

	def all () {
		return continuousIntegrationInfoProviderServiceLoader.asList() << new UnknownContinuousIntegrationProvider();
	}
			
	ContinuousIntegrationInfoProvider findProvider(project) {

		def provider = continuousIntegrationInfoProviderServiceLoader.find { it.supports(project) }
       
        if (provider) {
            return provider
        } else {
            return new UnknownContinuousIntegrationProvider()
        }
	}

}
