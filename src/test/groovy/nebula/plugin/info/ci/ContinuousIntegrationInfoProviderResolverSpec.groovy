package nebula.plugin.info.ci

import nebula.plugin.info.InfoBrokerPlugin
import nebula.plugin.info.InfoBrokerPlugin.ManifestEntry
import nebula.plugin.info.basic.BasicInfoPlugin
import nebula.test.ProjectSpec

import org.gradle.api.NamedDomainObjectContainer

import spock.lang.Ignore

import java.util.concurrent.atomic.AtomicBoolean

class ContinuousIntegrationInfoProviderResolverSpec extends ProjectSpec {

	def 'get all configured info providers'() {
		when:
		def resolver = new ContinuousIntegrationInfoProviderResolver()

		then:
		def providers = resolver.all()
		providers != null
		providers.size() == 2
		providers[0].getClass().equals(JenkinsProvider.class)
		providers[1].getClass().equals(UnknownContinuousIntegrationProvider.class)
	}

	def 'get Jenkins provider if running on Jenkins'() {
		when:
		def onJenkins = System.getenv('BUILD_NUMBER') && System.getenv('JOB_NAME')
		def resolver = new ContinuousIntegrationInfoProviderResolver()

		then:
		def provider = resolver.findProvider(project)
		if(onJenkins) {
			provider.getClass().equals(JenkinsProvider.class)
		} else {
			provider.getClass().equals(UnknownContinuousIntegrationProvider.class)
		}
	}
}
