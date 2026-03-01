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

package net.linguica.gradle.maven.settings.test.common

import io.github.mhoffrog.gradle.maven.settings.utils.PluginResourcesUtil
import org.apache.maven.settings.io.DefaultSettingsWriter
import org.apache.maven.settings.io.SettingsWriter
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir

abstract class AbstractCommonMavenSettingsTest {

    protected static final String PLUGIN_ID = "${PluginResourcesUtil.pluginId}"

    // Testdata constants
    static final String TEST_MIRROR_ID = 'myrepo'
    static final String TEST_MIRROR_URL = 'http://maven.foo.bar'
    static final String TEST_PROFILE_ID = 'myprofile'
    static final String TEST_PROPERTY_KEY = 'myprop'
    static final String TEST_PROPERTY_VALUE = 'true'
    static final String TEST_USER_NAME = 'first.last'
    static final String TEST_USER_PASSWORD = 'secret'

    Project project
    URI gradleRuntimeDefaultMavenLocalURI

    @TempDir
    private File mavenLocalRootDir

    @TempDir
    private File testRootDir
    private File mavenSettingsDir
    private File mavenSettingsFile

    @BeforeEach
    void createSettingsXml() {
        mavenSettingsDir = new File(testRootDir, '.m2')
        mavenSettingsDir.mkdirs()
        mavenSettingsFile = new File(mavenSettingsDir, 'settings.xml')
        project = new ProjectBuilder().build()
        // Use another project to get the default maven local repo URI,
        // so that the test project configuration is not affected.
        new ProjectBuilder().build().repositories.mavenLocal { mavenLocalRepo ->
            this.gradleRuntimeDefaultMavenLocalURI = mavenLocalRepo.url
        }
    }

    @AfterEach
    void deleteSettingsXml() {
        mavenSettingsFile.delete()
        project = null
    }

    File getMavenLocalTestDir() {
        return new File(mavenLocalRootDir, ".m2/test_repo")
    }

    String getMavenLocalTestDirName() {
        return getMavenLocalTestDir().canonicalPath.replace('\\', '/')
    }

    File getMavenSettingsFile() {
        return mavenSettingsFile
    }

    String getMavenSettingsFileName() {
        return getMavenSettingsFile().canonicalPath.replace('\\', '/')
    }

    void withMavenSettings(@DelegatesTo(org.apache.maven.settings.Settings) Closure configureClosure) {
        org.apache.maven.settings.Settings mavenSettings = new org.apache.maven.settings.Settings()
        project.configure(mavenSettings, configureClosure)
        SettingsWriter writer = new DefaultSettingsWriter()
        writer.write(mavenSettingsFile, null, mavenSettings)
    }

}
