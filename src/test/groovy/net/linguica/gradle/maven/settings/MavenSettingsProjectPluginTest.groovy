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
import org.apache.maven.settings.Mirror
import org.apache.maven.settings.Profile
import org.apache.maven.settings.Server
import org.junit.jupiter.api.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class MavenSettingsProjectPluginTest extends AbstractMavenSettingsTest {

    @Test
    void applyMavenSettingsProjectPlugin() {
        project.with {
            apply plugin: "${PLUGIN_ID}"
        }

        assertTrue(project.plugins.hasPlugin(MavenSettingsPlugin.class))
    }

    @Test
    void declareGlobalMirror() {
        withMavenSettings {
            mirrors.add new Mirror(id: TEST_MIRROR_ID, mirrorOf: '*', url: TEST_MIRROR_URL)
        }

        addPluginWithSettings()

        project.with {
            repositories {
                mavenLocal()
                mavenCentral()
            }
        }

        project.evaluate()

        assertThat(project.repositories, hasSize(2))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo(TEST_MIRROR_ID))))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo(GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME))))
    }

    @Test
    void respectsMirrorExcludes() {
        withMavenSettings {
            mirrors.add new Mirror(id: TEST_MIRROR_ID, mirrorOf: '*,!some-repo', url: TEST_MIRROR_URL)
        }

        addPluginWithSettings()

        project.with {
            repositories {
                mavenLocal()
                mavenCentral()
                maven {
                    name = "some-repo"
                    url = "https://example.com"
                }
                maven {
                    name = "some-other-repo"
                    url = "https://example.com"
                }
            }
        }

        project.evaluate()

        assertThat(project.repositories, hasSize(3))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo(TEST_MIRROR_ID))))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo(GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME))))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo('some-repo'))))
    }

    @Test
    void declareExternalMirrorWithFileRepo() {
        withMavenSettings {
            mirrors.add new Mirror(id: TEST_MIRROR_ID, mirrorOf: 'external:*', url: TEST_MIRROR_URL)
        }

        addPluginWithSettings()

        project.with {
            repositories {
                mavenLocal()
                mavenCentral()
                maven {
                    name = 'myLocal'
                    url = uri(new File(project.layout.buildDirectory.asFile.get(), '.m2'))
                }
            }
        }

        project.evaluate()

        assertThat(project.repositories, hasSize(3))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo(TEST_MIRROR_ID))))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo(GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME))))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo('myLocal'))))
    }

    @Test
    void declareExternalMirrorWithLocalhostRepo() {
        withMavenSettings {
            mirrors.add new Mirror(id: TEST_MIRROR_ID, mirrorOf: 'external:*', url: TEST_MIRROR_URL)
        }

        addPluginWithSettings()

        project.with {
            repositories {
                mavenLocal()
                mavenCentral()
                maven {
                    name = 'myLocal'
                    url = "http://localhost/maven"
                }
            }
        }

        project.evaluate()

        assertThat(project.repositories, hasSize(3))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo(TEST_MIRROR_ID))))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo(GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME))))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo('myLocal'))))
    }

    @Test
    void declareMavenCentralMirror() {
        withMavenSettings {
            mirrors.add new Mirror(id: TEST_MIRROR_ID, mirrorOf: 'central', url: TEST_MIRROR_URL)
        }

        addPluginWithSettings()

        project.with {
            repositories {
                mavenLocal()
                mavenCentral()
                maven {
                    name = 'myRemote'
                    url = "https://maven.foobar.org/repo"
                }
            }
        }

        project.evaluate()

        assertThat(project.repositories, hasSize(3))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo(TEST_MIRROR_ID))))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo(GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME))))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo('myRemote'))))
    }

    @Test
    void declareMavenCentralMirrorWithoutCentralRepo() {
        withMavenSettings {
            mirrors.add new Mirror(id: TEST_MIRROR_ID, mirrorOf: 'central', url: TEST_MIRROR_URL)
        }

        addPluginWithSettings()

        project.with {
            repositories {
                mavenLocal()
                maven {
                    name = 'myRemote'
                    url = "https://maven.foobar.org/repo"
                }
            }
        }

        project.evaluate()

        assertThat(project.repositories, hasSize(2))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo(GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME))))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo('myRemote'))))
    }

    @Test
    void profileActiveWithSettingsActiveProfile() {
        def props = new Properties()
        props.setProperty(TEST_PROPERTY_KEY, TEST_PROPERTY_VALUE)

        withMavenSettings {
            profiles.add new Profile(id: TEST_PROFILE_ID, properties: props)
            activeProfiles = [TEST_PROFILE_ID]
        }

        addPluginWithSettings()

        project.evaluate()

        assertThat(project.properties, hasEntry(TEST_PROPERTY_KEY, TEST_PROPERTY_VALUE))
    }

    @Test
    void profileActiveWithExtensionActiveProfile() {
        def props = new Properties()
        props.setProperty(TEST_PROPERTY_KEY, TEST_PROPERTY_VALUE)

        withMavenSettings {
            profiles.add new Profile(id: TEST_PROFILE_ID, properties: props)
        }

        addPluginWithSettings()

        project.with {
            mavenSettings {
                activeProfiles = [TEST_PROFILE_ID]
            }
        }

        project.evaluate()

        assertThat(project.properties, hasEntry(TEST_PROPERTY_KEY, TEST_PROPERTY_VALUE))
    }

    @Test
    void credentialsAddedToNamedRepository() {
        withMavenSettings {
            servers.add new Server(id: 'central', username: TEST_USER_NAME, password: TEST_USER_PASSWORD)
        }

        addPluginWithSettings()

        project.with {
            repositories {
                maven {
                    name = 'central'
                    url = 'https://repo1.maven.org/maven2/'
                }
            }
        }

        project.evaluate()

        assertEquals(TEST_USER_NAME, project.repositories.central.credentials.username)
        assertEquals(TEST_USER_PASSWORD, project.repositories.central.credentials.password)
    }

    @Test
    void credentialsOnlyAddedToMavenRepositories() {
        withMavenSettings {
            servers.add new Server(id: 'flat', username: TEST_USER_NAME, password: TEST_USER_PASSWORD)
        }

        addPluginWithSettings()

        project.with {
            repositories {
                flatDir {
                    name = 'flat'
                    dirs '.'
                }
            }
        }

        project.evaluate()
    }

    @Test
    void credentialsAddedToPublishingRepository() {
        withMavenSettings {
            servers.add new Server(id: 'central', username: TEST_USER_NAME, password: TEST_USER_PASSWORD)
        }

        addPluginWithSettings()

        project.with {
            apply plugin: 'maven-publish'

            publishing {
                repositories {
                    maven {
                        name = 'central'
                        url = 'https://repo1.maven.org/maven2/'
                    }
                }
            }
        }

        project.evaluate()

        assertEquals(TEST_USER_NAME, project.publishing.repositories.central.credentials.username)
        assertEquals(TEST_USER_PASSWORD, project.publishing.repositories.central.credentials.password)
    }

    @Test
    void replaceMavenLocalURLWithConfigFromSettingsFile() {
        withMavenSettings {
            localRepository = getMavenLocalTestDirName()
        }

        addPluginWithSettings()

        project.with {
            repositories {
                mavenLocal()
            }
        }

        project.evaluate()

        assertThat(project.repositories, hasSize(1))
        assertThat(project.repositories, hasItem(hasProperty('name', equalTo(GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME))))
        assertThat(project.repositories, hasItem(hasProperty('url', equalTo(getMavenLocalTestDir().toURI()))))
    }

}
