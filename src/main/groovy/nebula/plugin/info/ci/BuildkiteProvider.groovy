/*
 * Copyright 2014-2026 Netflix, Inc.
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

import groovy.transform.CompileStatic
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jspecify.annotations.NullMarked

@CompileStatic
@NullMarked
class BuildkiteProvider extends AbstractContinuousIntegrationProvider {

    BuildkiteProvider(ProviderFactory providerFactory) {
        super(providerFactory)
    }

    @Override
    boolean supports() {
        environmentVariable('BUILDKITE').isPresent()
    }

    /**
     * The build number. This number increases with every build, and is guaranteed to be unique within each pipeline.
     */
    @Override
    Provider<String> buildNumber() {
        environmentVariable('BUILDKITE_BUILD_NUMBER')
    }

    /**
     * The UUID of the build.
     */
    @Override
    Provider<String> buildId() {
        environmentVariable('BUILDKITE_BUILD_ID')
    }

    /**
     * The url for this build on Buildkite.
     */
    @Override
    Provider<String> buildUrl() {
        environmentVariable('BUILDKITE_BUILD_URL')
    }

    /**
     * Extract the host from BUILDKITE_BUILD_URL.
     */
    @Override
    Provider<String> host() {
        return environmentVariable('BUILDKITE_BUILD_URL')
                .map { url ->
                    int end = url.indexOf('/', url.indexOf('://') + 3)
                    end == -1 ? url : url.substring(0, end)
                }
    }

    /**
     * The organization slug and pipeline slug on Buildkite as used in URLs.
     *
     * <p>A "job" in this plugin is most closely aligned to a <em>pipeline</em> in Buildkite.
     * See <a href="https://buildkite.com/docs/pipelines/getting-started#create-a-new-pipeline">creating a pipeline</a>.
     * Each trigger of a pipeline becomes one build.
     *
     * <p>Job-Name includes the organization to make it globally unique.
     * Each pipeline name must be unique within an organization.
     */
    @Override
    Provider<String> job() {
        def orgSlug = environmentVariable('BUILDKITE_ORGANIZATION_SLUG')
        def pipelineSlug = environmentVariable('BUILDKITE_PIPELINE_SLUG')
        return orgSlug.zip(pipelineSlug) { org, pipeline -> "${org}/${pipeline}".toString() }
    }
}
