/*
 * Copyright 2026 (c) mhoffrog.
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

package io.github.mhoffrog.gradle.maven.settings.utils

class PluginResourcesUtil {

    private static final Properties pluginProperties

    private static final String PLUGIN_PROPERTIES_RESOURCE_PATH = "/maven-settings-plugin-expanded.properties"

    static {
        try (final InputStream inputStream = PluginResourcesUtil.class.getResourceAsStream(PLUGIN_PROPERTIES_RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Plugin properties resource not found: ${PLUGIN_PROPERTIES_RESOURCE_PATH}")
            }
            pluginProperties = new Properties()
            pluginProperties.load(inputStream)
            pluginProperties.stringPropertyNames().each { key ->
                Object val = pluginProperties[key]
                if (val instanceof String) {
                    pluginProperties[key] = val.trim()
                }
            }
        }
    }

    static String getPluginVersion() {
        return getProperty("plugin.version", true)
    }

    static String getPluginGroupId() {
        return getProperty("plugin.mavenGroupId", true)
    }

    static String getPluginArtifactId() {
        return getProperty("plugin.mavenArtifactId", true)
    }

    static String getPluginId() {
        return getProperty("plugin.id", true)
    }

    static String getPluginNameInLogPrefix() {
        return getProperty("plugin.nameInLogPrefix", true)
    }

    static String getPluginBuildDirectory() {
        return getProperty("plugin.buildDirectory", true)
    }

    static String getGitCommitIdShort() {
        String gitCommitIdFull = getGitCommitIdFull()
        int shortLen = 7
        return gitCommitIdFull ? gitCommitIdFull.substring(0, Math.min(shortLen, gitCommitIdFull.length())) : ''
    }

    static String getGitCommitIdFull() {
        return getProperty("git.commit.id", false) ?: ''
    }

    static String getProperty(String key, boolean isMandatory = false) {
        String ret = pluginProperties.getProperty(key)
        if (!ret && isMandatory) {
            throw new IllegalStateException("Mandatory plugin property '${key}' is missing or empty in resource: ${PLUGIN_PROPERTIES_RESOURCE_PATH}")
        }
        return ret
    }
}
