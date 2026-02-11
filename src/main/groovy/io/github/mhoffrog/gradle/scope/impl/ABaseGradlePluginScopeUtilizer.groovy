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

package io.github.mhoffrog.gradle.scope.impl

import io.github.mhoffrog.gradle.scope.IGradlePluginScopeUtilizer
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.publish.PublishingExtension

/**
 * Abstract base implementation of the {@link IGradlePluginScopeUtilizer} interface
 * that provides common functionality for utilizing Gradle plugin scopes.
 */
abstract class ABaseGradlePluginScopeUtilizer<T extends ExtensionAware> implements IGradlePluginScopeUtilizer<T> {

    private final T scope;

    private final Logger logger;

    ABaseGradlePluginScopeUtilizer(T scope) {
        this.scope = scope
        this.logger = isProjectScope() ? ((Project) getScope()).logger : Logging.getLogger(this.class)
    }

    T getScope() {
        return scope
    }

    boolean isProjectScope() {
        return getScope() instanceof Project
    }

    boolean isSettingsScope() {
        return getScope() instanceof Settings
    }

    /**
     * Gets the logger for the Gradle plugin scope.
     *
     * @return the logger for the Gradle plugin scope.
     */
    Logger getLogger() {
        return logger;
    };

    @Override
    Map getProperties() {
        return getScope().properties
    }

    @Override
    ExtensionContainer getExtensions() {
        return getScope().extensions
    }

    @Override
    boolean isRepositoriesConfigured() {
        RepositoryHandler repositories = getRepositories()
        return repositories != null && !repositories.empty
    }

    @Override
    RepositoryHandler getPushRepositories() {
        return getExtensions().findByType(PublishingExtension)?.repositories
    }

    @Override
    boolean isPushRepositoriesConfigured() {
        RepositoryHandler repositories = getPushRepositories()
        return repositories != null && !repositories.empty
    }

    @Override
    boolean isPluginManagementRepositoriesConfigured() {
        RepositoryHandler repositories = getPluginManagementRepositories()
        return repositories != null && !repositories.empty
    }

}