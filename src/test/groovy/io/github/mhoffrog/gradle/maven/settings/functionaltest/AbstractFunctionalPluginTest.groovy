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

import io.github.mhoffrog.gradle.maven.settings.MavenConstants
import net.linguica.gradle.maven.settings.test.common.AbstractCommonMavenSettingsTest
import net.linguica.gradle.maven.settings.test.common.beans.RepoCredentials
import net.linguica.gradle.maven.settings.test.common.beans.RepositoryBean
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir

import static org.junit.jupiter.api.Assertions.assertTrue

abstract class AbstractFunctionalPluginTest extends AbstractCommonMavenSettingsTest {

    private String pluginManagementRepositoriesBlock = ""

    private String dependencyRepositoriesBlock = ""

    private List<String> mirrorExclusions = []

    private boolean isAddDefaultMirrorExclusions = false

    private List<String> activeProfiles = []

    // ====== Expected results for assertions ======
    private List<RepositoryBean> effectivePluginManagementRepositories = []

    private List<RepositoryBean> effectiveDependencyRepositories = []

    private String expectedProfilePropertyName

    private String expectedProfilePropertyValue

    private List<RepoCredentials> expectedCredentials = []

    @TempDir
    private File projectDir

    private File getGradleSettingsFile() {
        return new File(projectDir, "settings.gradle")
    }

    private File getBuildFile() {
        return new File(projectDir, "build.gradle")
    }

    AbstractFunctionalPluginTest withPluginManagementRepositoriesBlock(String pluginManagementRepositoriesBlock) {
        this.pluginManagementRepositoriesBlock = pluginManagementRepositoriesBlock
        return this
    }

    AbstractFunctionalPluginTest withDependencyRepositoriesBlock(String repositoriesBlock) {
        this.dependencyRepositoriesBlock = repositoriesBlock
        return this
    }

    AbstractFunctionalPluginTest withMirrorExclusions(List<String> mirrorExclusions) {
        this.mirrorExclusions = mirrorExclusions
        return this
    }

    AbstractFunctionalPluginTest withAddDefaultMirrorExclusions(boolean isAddDefaultMirrorExclusions) {
        this.isAddDefaultMirrorExclusions = isAddDefaultMirrorExclusions
        return this
    }

    AbstractFunctionalPluginTest withActiveProfiles(List<String> activeProfiles) {
        this.activeProfiles = activeProfiles
        return this
    }

    AbstractFunctionalPluginTest expectEffectivePluginManagementRepositories(List<RepositoryBean> effectivePluginManagementRepositories) {
        this.effectivePluginManagementRepositories = effectivePluginManagementRepositories
        return this
    }

    AbstractFunctionalPluginTest expectEffectiveDependencyRepositories(List<RepositoryBean> effectiveDependencyRepositories) {
        this.effectiveDependencyRepositories = effectiveDependencyRepositories
        return this
    }

    AbstractFunctionalPluginTest expectProfileProperty(String expectedProfilePropertyName, String expectedProfilePropertyValue) {
        this.expectedProfilePropertyName = expectedProfilePropertyName
        this.expectedProfilePropertyValue = expectedProfilePropertyValue
        return this
    }

    AbstractFunctionalPluginTest expectCredentials(List<RepoCredentials> expectedCredentials) {
        this.expectedCredentials = expectedCredentials
        return this
    }

    List<String> getMirrorExclusions() {
        return mirrorExclusions
    }

    boolean isAddDefaultMirrorExclusions() {
        return isAddDefaultMirrorExclusions
    }

    void performTest(boolean isPrintSettingsGradleFileContent = false) {
        gradleSettingsFile.write(GradleSettingsFileTemplate.create()
                .withMavenSettingsFileName(mavenSettingsFileName)
                .withPluginManagementRepositoriesBlock(pluginManagementRepositoriesBlock)
                .withDependencyRepositoriesBlock(dependencyRepositoriesBlock)
                .withMirrorExclusions(mirrorExclusions)
                .withIsAddDefaultMirrorExclusions(isAddDefaultMirrorExclusions)
                .withProfilePropertyName(this.expectedProfilePropertyName)
                .withActiveProfiles(activeProfiles)
                .getGradleSettingFileContent())
        if (!gradleSettingsFile.exists()) {
            throw new IllegalStateException("Failed to create Gradle settings file at: ${gradleSettingsFile.canonicalPath}")
        }
        if (isPrintSettingsGradleFileContent) {
            println("====== settings.gradle ======:\n${gradleSettingsFile.text}\n====== EOF ======")
        }

        buildFile.write("""
             tasks.register('nop') {}
         """.stripIndent())
        if (!buildFile.exists()) {
            throw new IllegalStateException("Failed to create Gradle build file at: ${buildFile.canonicalPath}")
        }

        // Act: execute the task in an isolated Gradle build
        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()      // picks up your plugin-under-test from the test classpath
                .withArguments("nop", "--info")
        //.forwardOutput()
                .build()

        // Assert: verify console output and successful task outcome
        println("====== Result: ======\n${result.output}\n====== EndOfResult ======")
        assertTrue(result.task(":nop")?.outcome == TaskOutcome.UP_TO_DATE)
        "TESTRESULT: Effective pluginManagement repositories: [${this.effectivePluginManagementRepositories.size()}]".with { expected ->
            assertTrue(result.output.contains(expected), "Expected output to contain: ${expected}")
        }
        "TESTRESULT: Effective dependency repositories: [${this.effectiveDependencyRepositories.size()}]".with { expected ->
            assertTrue(result.output.contains(expected), "Expected output to contain: ${expected}")
        }
        this.effectivePluginManagementRepositories.each { repo ->
            "TESTRESULT: Effective pluginManagement repository: ${repo.name} (${repo.url})".with { expected ->
                assertTrue(result.output.contains(expected), "Expected output to contain: ${expected}")
            }
        }
        this.effectiveDependencyRepositories.each { repo ->
            "TESTRESULT: Effective dependency repository: ${repo.name} (${repo.url})".with { expected ->
                assertTrue(result.output.contains(expected), "Expected output to contain: ${expected}")
            }
        }
        if (this.expectedProfilePropertyName) {
            "TESTRESULT: Profile property: '${this.expectedProfilePropertyName}' value: '${expectedProfilePropertyValue}'".with { expected ->
                assertTrue(result.output.contains(expected), "Expected output to contain: ${expected}")
            }
        }
        expectedCredentials.each { credentials ->
            "TESTRESULT: ${credentials.repoContext} ${credentials.repoName} credentials: (${credentials.username}, ${credentials.password})".with { expected ->
                assertTrue(result.output.contains(expected), "Expected output to contain: ${expected}")
            }
        }
    }

}