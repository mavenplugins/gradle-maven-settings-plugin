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

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.provider.Provider

class ExtensionAwareUtil {

    // List of Gradle Project or Settings properties that are deprecated in Gradle 9
    // and should be ignored to avoid deprecation warnings.
    // See:
    //   org.gradle.api.plugins.internal.NaggingJavaPluginConvention
    //   org.gradle.api.plugins.BasePluginConvention
    //   org.gradle.api.plugins.JavaPluginConvention
    private static final List<String> GRADLE_DEPRECATED_EXTENSIONAWARE_PROPS = [
            'archivesBaseName',
            'archivesName',
            'autoTargetJvmDisabled',
            'convention',
            'distsDirectory',
            'distsDirName',
            'docsDir',
            'docsDirName',
            'libsDirectory',
            'libsDirName',
            'manifest',
            'project',
            'reportsDir',
            'sourceSets',
            'sourceCompatibility',
            'targetCompatibility',
            'testReportDir',
            'testReportDirName',
            'testResultsDir',
            'testResultsDirName'
    ]

    private static boolean hasExtensionsExtraPropertiesOnly(ExtensionAware extensionAware) {
        return !(extensionAware instanceof Project)
    }

    private static Set<String> collectExtensionAwarePropertyKeys(ExtensionAware extensionAware) {
        Set<String> propertyKeys = hasExtensionsExtraPropertiesOnly(extensionAware) ? Collections.emptySet() : extensionAware.properties.keySet()
        ExtraPropertiesExtension extraPropertiesExtension = extensionAware.extensions?.extraProperties
        Set<String> extraPropertyKeys = extraPropertiesExtension?.properties?.keySet()
        if (extraPropertyKeys) {
            propertyKeys += extraPropertyKeys
            propertyKeys = propertyKeys.toSet()
        }
        return propertyKeys
    }

    /**
     * Collects all properties of the given ExtensionAware (Project or Settings) that are of simple types (String, Number, Boolean)
     * or File (converted to absolute path), and returns them as a Map. Complex properties are skipped with a debug log.
     * Deprecated Gradle properties are also skipped to avoid deprecation warnings.
     */
    static Map<String, Serializable> getSerializableProperties(
            ExtensionAware extensionAware, Logger logger) {
        Set<String> propertyKeys = collectExtensionAwarePropertyKeys(extensionAware)
        Map<String, Serializable> extensionAwareProperties = [:]
        propertyKeys.each { key ->
            // Avoid useless Gradle deprecation log
            if (GRADLE_DEPRECATED_EXTENSIONAWARE_PROPS.contains(key)) {
                logger.debug("ExtensionAwareUtil: IGNORE deprecated Gradle ExtensionAware property '${key}'.")
                return // continue
            }
            logger.debug("ExtensionAwareUtil: LOOK UP property '${key}'.")
            Object value = hasExtensionsExtraPropertiesOnly(extensionAware) ?
                    extensionAware.extensions?.extraProperties[key]
                    : (extensionAware instanceof Project) ? extensionAware.findProperty(key)
                    : extensionAware.properties[key]
            if (value instanceof Provider) {
                Provider p = (Provider) value
                value = p.present ? p.get() : null
            }
            if (value instanceof File) {
                value = ((File) value).absolutePath
            }
            if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
                logger.debug("ExtensionAwareUtil: CAPTURE property '${key}'.")
                extensionAwareProperties.put(key, value instanceof Serializable ? (Serializable) value : value.toString())
            } else if (value) {
                logger.debug("ExtensionAwareUtil: SKIP complex property '${key}' of type '${value.getClass().name}'.")
            } else {
                logger.debug("ExtensionAwareUtil: SKIP null or empty property '${key}'.")
            }
        }
        return extensionAwareProperties
    }
}
