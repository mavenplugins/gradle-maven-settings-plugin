/*
 * Copyright 2014 the original author or authors.
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

package net.linguica.gradle.maven.settings

import io.github.mhoffrog.gradle.maven.settings.GradleConstants
import io.github.mhoffrog.gradle.maven.settings.MavenConstants
import io.github.mhoffrog.gradle.maven.settings.scope.IGradlePluginScopeUtilizer
import org.gradle.api.Project

class MavenSettingsPluginExtension {

    private final IGradlePluginScopeUtilizer scopeUtilizer;

    /**
     * List repo names to exclude from mirror application by default.
     */
    private static final List<String> DEFAULT_MIRROR_EXCLUSIONS = [
            GradleConstants.GRADLE_PLUGIN_PORTAL_REPO_NAME,
            GradleConstants.GOOGLE_REPO_NAME,
            GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME,
            GradleConstants.MAVEN_CENTRAL_SNAPSHOTS_REPO_NAME
    ]

    /**
     * Name of settings file to use. String is evaluated using {@link org.gradle.api.Project#file(java.lang.Object)}.
     * Defaults to $USER_HOME/.m2/settings.xml.
     */
    String userSettingsFileName = MavenConstants.DEFAULT_MAVEN_SETTINGS_FILE_NAME

    /**
     * List of profile ids to treat as active.
     */
    String[] activeProfiles = []

    /**
     * List of repository names to exclude from mirror application.
     * Defaults to {@link #DEFAULT_MIRROR_EXCLUSIONS}.
     */
    List<String> mirrorExclusions = getDefaultMirrorExclusions()

    /**
     * Flag indicating whether or not Gradle project properties should be exported for the purposes of settings file
     * property interpolation and profile activation. Defaults to true.
     */
    boolean exportGradleProps = true

    /**
     * Flag indicating whether or not repositories defined in the settings file should be added to the Gradle project.
     * Defaults to true.
     */
    boolean addRepositories = true

    /**
     * Flag indicating whether or not plugin repositories defined in the settings file should be added to the Gradle pluginManagement.
     * Defaults to false, as Maven plugin repositories are typically not needed for Gradle pluginManagement.
     */
    boolean addPluginRepositories = false

    MavenSettingsPluginExtension(IGradlePluginScopeUtilizer scopeUtilizer) {
        this.scopeUtilizer = scopeUtilizer
    }

    public File getUserSettingsFile() {
        return scopeUtilizer.scope instanceof Project ? ((Project) scopeUtilizer.scope).file(userSettingsFileName) : new File(userSettingsFileName)
    }

    public boolean isNonDefaultUserSettingsFileConfigured() {
        File configured = getUserSettingsFile()?.canonicalFile
        File defaultFile = MavenConstants.DEFAULT_MAVEN_SETTINGS_FILE.canonicalFile
        return configured != defaultFile
    }

    /**
     * Return a copy of the default mirror exclusions list, to prevent accidental modification of the original list.
     */
    public static List<String> getDefaultMirrorExclusions() {
        return new ArrayList(DEFAULT_MIRROR_EXCLUSIONS)
    }
}
