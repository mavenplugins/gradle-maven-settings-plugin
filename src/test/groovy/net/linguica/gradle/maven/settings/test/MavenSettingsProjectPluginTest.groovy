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

package net.linguica.gradle.maven.settings.test

import io.github.mhoffrog.gradle.maven.settings.GradleConstants
import net.linguica.gradle.maven.settings.MavenSettingsPlugin
import org.apache.maven.settings.*
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.MavenRepositoryContentDescriptor
import org.junit.jupiter.api.Test

import java.util.function.Consumer

import static org.assertj.core.api.Assertions.assertThat
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
    void shouldMirrorAllRepos() {
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

        assertThat(project.repositories.names).containsOnly(TEST_MIRROR_ID, GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME)
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

        assertThat(project.repositories.names).containsOnly(TEST_MIRROR_ID, GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME, "some-repo")
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

        assertThat(project.repositories.names).containsOnly(TEST_MIRROR_ID, GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME, 'myLocal')
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

        assertThat(project.repositories.names).containsOnly(TEST_MIRROR_ID, GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME, 'myLocal')
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

        assertThat(project.repositories.names).containsOnly(TEST_MIRROR_ID, GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME, 'myRemote')
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

        assertThat(project.repositories.names).containsOnly(GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME, 'myRemote')
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

        assertThat(project.properties).containsEntry(TEST_PROPERTY_KEY, TEST_PROPERTY_VALUE)
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

        assertThat(project.properties).containsEntry(TEST_PROPERTY_KEY, TEST_PROPERTY_VALUE)
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

        assertThat(project.repositories.findByName('flat')).isInstanceOf(FlatDirectoryArtifactRepository.class)
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

        assertThat(project.repositories.names).containsOnly(GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME)
        assertThat(project.repositories*.url).containsOnly(getMavenLocalTestDir().toURI())
    }

    @Test
    void shouldAddRepositoriesFromActiveProfiles() {
        withMavenSettings {

            profiles.add new Profile(id: "profile1",
                    repositories: [new Repository(
                            id: "repoFromProfile1",
                            url: "http://maven.repo1.com",
                            releases: new RepositoryPolicy(enabled: false),
                            snapshots: new RepositoryPolicy(enabled: true))
                    ])
            profiles.add new Profile(id: "profile2",
                    repositories: [new Repository(
                            id: "repoFromProfile2",
                            url: "http://maven.repo2.com")
                    ])
            profiles.add new Profile(id: "profile3",
                    repositories: [new Repository(
                            id: "repoFromProfile3",
                            url: "http://maven.repo3.com")
                    ])

            servers.add new Server(id: "repoFromProfile2", username: TEST_USER_NAME, password: TEST_USER_PASSWORD)

            activeProfiles = ["profile1", "profile2"]
        }

        addPluginWithSettings()

        project.with {
            repositories {
                mavenLocal()
                mavenCentral()
                maven {
                    it.name = "custom1"
                    it.url = uri("https://maven.custom.com")
                }
            }
        }

        project.evaluate()

        //No profile 3, profile is not activated
        assertThat(project.repositories.names).containsOnly(
                "repoFromProfile1",
                "repoFromProfile2",
                GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME,
                GradleConstants.GRADLE_MAVEN_CENTRAL_REPO_NAME,
                "custom1")

        assertThat(project.repositories
                .withType(MavenArtifactRepository)
                .collect { (it as MavenArtifactRepository).url.toString() })
                .contains(
                        "https://repo.maven.apache.org/maven2/",
                        "https://maven.custom.com",
                        "http://maven.repo1.com",
                        "http://maven.repo2.com"
                )

        // repoFromProfile1 has snapshots enabled
        assertThat(project.repositories.getByName("repoFromProfile1") as MavenArtifactRepository)
        // Note: .satisfies requires casting the argument to Consumer - s. https://github.com/assertj/assertj/issues/2357
                .satisfies((Consumer) {
                    it.content {
                        assertThat(isReleasesOnly(it as MavenRepositoryContentDescriptor)).isFalse()
                        assertThat(isSnapshotsOnly(it as MavenRepositoryContentDescriptor)).isTrue()
                    }
                })

        assertThat(project.repositories.getByName("repoFromProfile2") as MavenArtifactRepository)
        // Note: .satisfies requires casting the argument to Consumer - s. https://github.com/assertj/assertj/issues/2357
                .satisfies((Consumer) {
                    assertThat(it.credentials.username).isEqualTo(TEST_USER_NAME)
                    assertThat(it.credentials.password).isEqualTo(TEST_USER_PASSWORD)
                })
    }

    @Test
    void mirrorsShouldOverrideReposWhenIdIsTheSame() {
        withMavenSettings {

            profiles.add new Profile(id: "profile1",
                    repositories: [new Repository(
                            id: "my.unique.repo",
                            url: "http://maven.repo1.com"
                    )])

            mirrors.add new Mirror(id: "my.unique.repo", mirrorOf: "*", url: "http://maven.mirror.com")

            activeProfiles = ["profile1"]
        }

        addPluginWithSettings()

        project.with {
            repositories {
                mavenCentral()
            }
        }

        project.evaluate()

        // mirror should replace the one from profile (even with same id)
        assertThat(project.repositories).hasSize(1).first()
        // Note: .satisfies requires casting the argument to Consumer - s. https://github.com/assertj/assertj/issues/2357
                .satisfies((Consumer) {
                    assertThat((it as MavenArtifactRepository).name).isEqualTo("my.unique.repo")
                    assertThat((it as MavenArtifactRepository).url).isEqualTo(new URI("http://maven.mirror.com"))
                })
    }

    @Test
    void shouldWorkWithReleaseOnlyProfile() {
        withMavenSettings {

            profiles.add new Profile(id: "profile1",
                    repositories: [new Repository(
                            id: "releaseOnly",
                            url: "http://maven.repo1.com",
                            releases: new RepositoryPolicy(enabled: true),
                            snapshots: new RepositoryPolicy(enabled: false))
                    ])

            activeProfiles = ["profile1"]
        }

        addPluginWithSettings()

        project.with {
            repositories {
                mavenCentral()
            }
        }

        project.evaluate()

        assertThat(project.repositories.names).containsOnly("releaseOnly", GradleConstants.GRADLE_MAVEN_CENTRAL_REPO_NAME)
        assertThat(project.repositories.getByName("releaseOnly") as MavenArtifactRepository)
        // Note: .satisfies requires casting the argument to Consumer - s. https://github.com/assertj/assertj/issues/2357
                .satisfies((Consumer) {
                    it.content {
                        assertThat(isReleasesOnly(it as MavenRepositoryContentDescriptor)).isTrue()
                        assertThat(isSnapshotsOnly(it as MavenRepositoryContentDescriptor)).isFalse()
                    }
                })
    }

    @Test
    void resolvePropertiesInRepoNameAndUrl() {
        def props = new Properties()
        props.setProperty('repoId', 'myTestRepo')
        props.setProperty('repoUrl', 'https://myTestHost/myTestRepo')
        props.setProperty('repoUrl2', 'https://myTestHost/myTestRepo2')

        withMavenSettings {
            servers.add new Server(id: 'myTestRepo', username: TEST_USER_NAME, password: TEST_USER_PASSWORD)
            servers.add new Server(id: 'myFixRepoId', username: TEST_USER_NAME + '2', password: TEST_USER_PASSWORD + '2')
            profiles.add new Profile(id: TEST_PROFILE_ID, properties: props)
        }

        addPluginWithSettings()

        project.with {
            mavenSettings {
                activeProfiles = [TEST_PROFILE_ID]
            }

            repositories {
                maven {
                    name = 'mavenSettings@repoId'
                    url = 'https://mavenSettings@repoUrl'
                }
                maven {
                    name = 'myFixRepoId'
                    url = 'https://mavenSettings@repoUrl2'
                }
            }
        }

        project.evaluate()

        assertThat(project.properties).containsEntry('repoId', 'myTestRepo')
        assertThat(project.properties).containsEntry('repoUrl', 'https://myTestHost/myTestRepo')
        assertThat(project.repositories.names).containsOnly('mavenSettings@repoId', 'myFixRepoId')
        assertThat(project.repositories.withType(MavenArtifactRepository)
                .collect { it.url.toString() })
                .contains(
                        new URI('https://myTestHost/myTestRepo').toString(),
                        new URI('https://myTestHost/myTestRepo2').toString()
                )
        assertEquals(TEST_USER_NAME, project.repositories.getByName('mavenSettings@repoId').credentials.username)
        assertEquals(TEST_USER_PASSWORD, project.repositories.getByName('mavenSettings@repoId').credentials.password)
        assertEquals(TEST_USER_NAME + '2', project.repositories.myFixRepoId.credentials.username)
        assertEquals(TEST_USER_PASSWORD + '2', project.repositories.myFixRepoId.credentials.password)
    }
}
