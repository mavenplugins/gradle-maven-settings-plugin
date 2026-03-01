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

    static String getProperty(String key, boolean isMandatory = false) {
        String ret = pluginProperties.getProperty(key)
        if (!ret && isMandatory) {
            throw new IllegalStateException("Mandatory plugin property '${key}' is missing or empty in resource: ${PLUGIN_PROPERTIES_RESOURCE_PATH}")
        }
        return ret
    }
}
