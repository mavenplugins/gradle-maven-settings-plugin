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

package net.linguica.gradle.maven.settings

import groovy.transform.CompileStatic
import io.github.mhoffrog.gradle.maven.settings.scope.IGradlePluginScopeUtilizer
import io.github.mhoffrog.gradle.maven.settings.scope.impl.GradlePluginProjectScopeUtilizer
import io.github.mhoffrog.gradle.maven.settings.scope.impl.GradlePluginSettingsScopeUtilizer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.PluginAware

@CompileStatic
class MavenSettingsPlugin extends AbstractMavenSettingsPlugin implements Plugin<PluginAware> {

    @Override
    void apply(PluginAware pluginAware) {
        IGradlePluginScopeUtilizer scopeUtilizer;
        if (pluginAware instanceof Project) {
            scopeUtilizer = new GradlePluginProjectScopeUtilizer((Project) pluginAware)
        } else if (pluginAware instanceof Settings) {
            scopeUtilizer = new GradlePluginSettingsScopeUtilizer((Settings) pluginAware)
        } else {
            throw new IllegalArgumentException("MavenSettingsPlugin can only be applied to a Settings or to a Project scope.")
        }
        apply(scopeUtilizer)
    }
}
