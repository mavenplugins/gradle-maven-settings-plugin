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

package io.github.mhoffrog.gradle.maven.settings.functionaltest

import static net.linguica.gradle.maven.settings.test.common.AbstractCommonMavenSettingsTest.PLUGIN_ID

class GradleSettingsFileTemplate {

    private String dependencyRepositoriesBlock = ""

    private String pluginManagementRepositoriesBlock = ""

    private String mavenSettingsFileName

    private final String pluginId = PLUGIN_ID

    private List<String> mirrorExclusions = []

    private boolean isAddDefaultMirrorExclusions = false

    private String profilePropertyName

    private List<String> activeProfiles = []

    static GradleSettingsFileTemplate create() {
        return new GradleSettingsFileTemplate()
    }

    private GradleSettingsFileTemplate() {
    }

    GradleSettingsFileTemplate withMavenSettingsFileName(String mavenSettingsFileName) {
        this.mavenSettingsFileName = mavenSettingsFileName
        return this
    }

    GradleSettingsFileTemplate withDependencyRepositoriesBlock(String dependencyRepositoriesBlock) {
        this.dependencyRepositoriesBlock = dependencyRepositoriesBlock
        return this
    }

    GradleSettingsFileTemplate withPluginManagementRepositoriesBlock(String pluginManagementRepositoriesBlock) {
        this.pluginManagementRepositoriesBlock = pluginManagementRepositoriesBlock
        return this
    }

    GradleSettingsFileTemplate withMirrorExclusions(List<String> mirrorExclusions) {
        this.mirrorExclusions = mirrorExclusions
        return this
    }

    GradleSettingsFileTemplate withIsAddDefaultMirrorExclusions(boolean isAddDefaultMirrorExclusions) {
        this.isAddDefaultMirrorExclusions = isAddDefaultMirrorExclusions
        return this
    }

    GradleSettingsFileTemplate withProfilePropertyName(String profilePropertyName) {
        this.profilePropertyName = profilePropertyName
        return this
    }

    GradleSettingsFileTemplate withActiveProfiles(List<String> activeProfiles) {
        this.activeProfiles = activeProfiles
        return this
    }

    String getGradleSettingFileContent() {
        return """
            pluginManagement {
                repositories {
                    ${this.pluginManagementRepositoriesBlock}
                }
            }

            plugins {
                id "${pluginId}"
            }
            
            // Helper for test result outputs
            void logTestResults(RepositoryHandler repositories, String context) {
                println("TESTRESULT: \${context} repositories: [\${repositories.names.size()}]")
                repositories.each { repo ->
                    if (repo instanceof org.gradle.api.artifacts.repositories.MavenArtifactRepository) {
                        println("TESTRESULT: \${context} repository: \${repo.name} (\${repo.url})")
                        if (repo.credentials?.username || repo.credentials?.password) {
                            println("TESTRESULT: \${context} \${repo.name} credentials: (\${repo.credentials?.username}, \${repo.credentials?.password})")
                        }
                    } else if (repo instanceof org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository) {
                        println("TESTRESULT: \${context} repository: \${repo.name} (dir) \${repo.dirs}")
                    } else {
                        println("TESTRESULT: \${context} repository: \${repo.name} (unknown type)")
                    }                     
                }
            }
            logTestResults(pluginManagement.repositories, "Original pluginManagement")
                        
            dependencyResolutionManagement {
                repositories {
                    ${this.dependencyRepositoriesBlock}
                }
            }
            logTestResults(dependencyResolutionManagement.repositories, "Original dependency")

            ${getMavenSettingsBlock()}

            gradle.settingsEvaluated {
                logTestResults(pluginManagement.repositories, "Effective pluginManagement")
                logTestResults(dependencyResolutionManagement.repositories, "Effective dependency")
                ${getProfilesPropertyResultLog()}
            }
        """.stripIndent()
    }

    private String getMavenSettingsBlock() {
        return """
            mavenSettings {
                userSettingsFileName = '${mavenSettingsFileName}'
                ${!activeProfiles.empty ? "activeProfiles = [${activeProfiles.collect { "'${it}'" }.join(', ')}]" : "// No activeProfiles configured"}
                ${!mirrorExclusions.empty ? "mirrorExclusions = ${isAddDefaultMirrorExclusions ? "defaultMirrorExclusions + " : ""}[${mirrorExclusions.collect { "'${it}'" }.join(', ')}]" : "// No mirrorExclusions configured"} 
            }
        """.stripIndent()
    }

    private String getProfilesPropertyResultLog() {
        if (profilePropertyName) {
            return """
                println("TESTRESULT: Profile property: '${profilePropertyName}' value: '\${properties['${profilePropertyName}']}'")
            """.stripIndent()
        }
        return ""
    }

}
