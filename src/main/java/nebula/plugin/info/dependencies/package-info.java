/*
 * Copyright 2026 Netflix, Inc.
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
/**
 * Dependency information collected locally and, when requested, aggregated across projects.
 *
 * <p>Apply the dependency-info collector and info broker in each participating
 * project. Use {@link nebula.plugin.info.dependencies.DependencyInfoExtension}
 * to select that project's report configurations. The {@code writeDependencyInfo}
 * task writes the local JSON file; {@code writeResolvedDependencies}, available on
 * a participating root, combines the project artifacts. Consume task output
 * providers so Gradle can track producer dependencies and configuration-cache inputs.
 *
 * <p>The explicit file reports and legacy callback reports share their key and
 * module notation formats, but differ in scope: file reports contain declared
 * selections, while the legacy map observes configurations resolved elsewhere.
 * Empty configurations add no configuration key; empty participating projects
 * still contribute a project entry. External transitive modules are included,
 * project components are omitted, and selected dependency resolution failures
 * prevent a file report from succeeding.
 *
 * <p><strong>Compatibility:</strong> the legacy {@code nebulaInfoDependencies}
 * property is now read-only, child reads are project-local, and child-only
 * application no longer creates root state. Evaluating participating legacy
 * entries with configuration cache active throws migration guidance. These are
 * behavioral breaking changes despite preserving the existing collector and
 * broker method signatures. Consumers requiring the old mutable shared map must
 * migrate before adopting this implementation; it is not a drop-in compatible update.
 */
package nebula.plugin.info.dependencies;
