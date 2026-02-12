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

package io.github.mhoffrog.gradle.scope


import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionContainer

/**
 * Interface for classes that utilize Gradle plugin scopes.
 */
interface IGradlePluginScopeUtilizer<T> {

    /**
     * Gets the scope of the Gradle plugin.
     *
     * @return the scope instance of the Gradle plugin like
     * {@link org.gradle.api.Project} or {@link org.gradle.api.initialization.Settings}.
     */
    T getScope()

    boolean isProjectScope()

    boolean isSettingsScope()

    /**
     * Gets the logger for the Gradle plugin scope.
     *
     * @return the logger for the Gradle plugin scope.
     */
    Logger getLogger()

    /**
     * Gets the properties for the Gradle plugin scope.
     *
     * @return the properties for the Gradle plugin scope.
     */
    Map getProperties()

    /**
     * Gets the extensions container for the Gradle plugin scope.
     *
     * @return the extensions container for the Gradle plugin scope.
     */
    ExtensionContainer getExtensions()

    /**
     * Gets the project directory for the Gradle plugin scope.
     *
     * @return the project directory for the Gradle plugin scope.
     */
    File getProjectDir()

    /**
     * Sets the plugin event closure to be performed.
     *
     * @param eventClosure the closure to be set within the plugin apply callback.
     */
    void setPluginEventClosure(Closure<Void> eventClosure)

    /**
     * Gets the repository handler for the Gradle plugin scope.
     *
     * @return the repository handler for the Gradle plugin scope.
     */
    RepositoryHandler getRepositories()

    /**
     * Checks if repositories are configured for the Gradle plugin scope.
     *
     * @return true if repositories are configured for the Gradle plugin scope, false otherwise.
     */
    boolean isRepositoriesConfigured()

    /**
     * Gets the repository handler for the Gradle plugin scope publishing repositories.
     *
     * @return the repository handler for the Gradle plugin scope publishing repositories.
     */
    RepositoryHandler getPublishingRepositories()

    /**
     * Checks if publishing repositories are configured for the Gradle plugin scope.
     *
     * @return true if publishing repositories are configured for the Gradle plugin scope, false otherwise.
     */
    boolean isPublishingRepositoriesConfigured()

    /**
     * Gets the repository handler for the Gradle plugin management repositories.
     *
     * @return the repository handler for the Gradle plugin management repositories.
     */
    RepositoryHandler getPluginManagementRepositories()

    /**
     * Checks if plugin management repositories are configured for the Gradle plugin scope.
     *
     * @return true if plugin management repositories are configured for the Gradle plugin scope, false otherwise.
     */
    boolean isPluginManagementRepositoriesConfigured()

}