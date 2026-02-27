/*
 * Copyright (c) 2026 mhoffrog.
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

package io.github.mhoffrog.gradle.maven.settings.scope.impl

import io.github.mhoffrog.gradle.maven.settings.scope.IGradlePluginScopeUtilizer
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.initialization.Settings

/**
 * Implementation of the {@link IGradlePluginScopeUtilizer} interface
 * for a Gradle {@link Settings} scope plugin.
 */
class GradlePluginSettingsScopeUtilizer extends ABaseGradlePluginScopeUtilizer<Settings> {

    GradlePluginSettingsScopeUtilizer(Settings scope) {
        super(scope)
    }

    @Override
    File getProjectDir() {
        return getScope().rootDir
    }

    @Override
    void setPluginEventClosure(Closure<Void> eventClosure) {
        getScope().gradle.settingsEvaluated(eventClosure)
    }

    @Override
    RepositoryHandler getRepositories() {
        return getScope().dependencyResolutionManagement?.repositories
    }

    @Override
    RepositoryHandler getPluginManagementRepositories() {
        return getScope().pluginManagement?.repositories
    }

}