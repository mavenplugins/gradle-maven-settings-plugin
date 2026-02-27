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

package io.github.mhoffrog.maven.settings.funtionaltest

import io.github.mhoffrog.gradle.maven.settings.GradleConstants
import io.github.mhoffrog.maven.settings.funtionaltest.beans.RepoCredentials
import io.github.mhoffrog.maven.settings.funtionaltest.beans.RepositoryBean
import org.apache.maven.settings.Mirror
import org.apache.maven.settings.Profile
import org.apache.maven.settings.Server
import org.junit.jupiter.api.Test

class FunctionalPluginSettingsScopeTest extends AbstractFunctionalPluginTest {

    @Test
    void declareGlobalMirror() {
        withMavenSettings {
            mirrors.add new Mirror(id: TEST_MIRROR_ID, mirrorOf: '*', url: TEST_MIRROR_URL)
        }
        withDependencyRepositoriesBlock("""
            mavenLocal()
            mavenCentral()
        """)
        expectEffectiveDependencyRepositories([new RepositoryBean(GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME, gradleRuntimeDefaultMavenLocalURI.toString()),
                                               new RepositoryBean(TEST_MIRROR_ID, TEST_MIRROR_URL)
        ])
        performTest()
    }

    @Test
    void respectsMirrorExcludes() {
        withMavenSettings {
            mirrors.add new Mirror(id: TEST_MIRROR_ID, mirrorOf: '*,!some-repo', url: TEST_MIRROR_URL)
        }
        withDependencyRepositoriesBlock("""
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
        """)
        expectEffectiveDependencyRepositories([new RepositoryBean(GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME, gradleRuntimeDefaultMavenLocalURI.toString()),
                                               new RepositoryBean(TEST_MIRROR_ID, TEST_MIRROR_URL),
                                               new RepositoryBean('some-repo', 'https://example.com')
        ])
        performTest()
    }

    @Test
    void declareExternalMirrorWithLocalhostRepo() {
        withMavenSettings {
            mirrors.add new Mirror(id: TEST_MIRROR_ID, mirrorOf: 'external:*', url: TEST_MIRROR_URL)
        }

        withDependencyRepositoriesBlock("""
            mavenLocal()
            mavenCentral()
            maven {
                name = 'myLocal'
                url = "http://localhost/maven"
            }
        """)
        expectEffectiveDependencyRepositories([new RepositoryBean(GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME, gradleRuntimeDefaultMavenLocalURI.toString()),
                                               new RepositoryBean(TEST_MIRROR_ID, TEST_MIRROR_URL),
                                               new RepositoryBean('myLocal', 'http://localhost/maven')
        ])
        performTest()
    }

    @Test
    void declareMavenCentralMirror() {
        withMavenSettings {
            mirrors.add new Mirror(id: TEST_MIRROR_ID, mirrorOf: 'central', url: TEST_MIRROR_URL)
        }

        withDependencyRepositoriesBlock("""
            mavenLocal()
            mavenCentral()
            maven {
                name = 'myRemote'
                url = "https://maven.foobar.org/repo"
            }
        """)
        expectEffectiveDependencyRepositories([new RepositoryBean(GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME, gradleRuntimeDefaultMavenLocalURI.toString()),
                                               new RepositoryBean(TEST_MIRROR_ID, TEST_MIRROR_URL),
                                               new RepositoryBean('myRemote', 'https://maven.foobar.org/repo')
        ])
        performTest()
    }

    @Test
    void declareMavenCentralMirrorWithoutCentralRepo() {
        withMavenSettings {
            mirrors.add new Mirror(id: TEST_MIRROR_ID, mirrorOf: 'central', url: TEST_MIRROR_URL)
        }

        withDependencyRepositoriesBlock("""
            mavenLocal()
            maven {
                name = 'myRemote'
                url = "https://maven.foobar.org/repo"
            }
        """)
        expectEffectiveDependencyRepositories([new RepositoryBean(GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME, gradleRuntimeDefaultMavenLocalURI.toString()),
                                               new RepositoryBean('myRemote', 'https://maven.foobar.org/repo')
        ])
        performTest()
    }

    @Test
    void profileActiveWithSettingsActiveProfile() {
        def props = new Properties()
        props.setProperty(TEST_PROPERTY_KEY, TEST_PROPERTY_VALUE)

        withMavenSettings {
            profiles.add new Profile(id: TEST_PROFILE_ID, properties: props)
            activeProfiles = [TEST_PROFILE_ID]
        }
        expectProfileProperty(TEST_PROPERTY_KEY, TEST_PROPERTY_VALUE)
        performTest()
    }

    @Test
    void profileActiveWithExtensionActiveProfile() {
        def props = new Properties()
        props.setProperty(TEST_PROPERTY_KEY, TEST_PROPERTY_VALUE)

        withMavenSettings {
            profiles.add new Profile(id: TEST_PROFILE_ID, properties: props)
        }
        withActiveProfiles([TEST_PROFILE_ID])
        expectProfileProperty(TEST_PROPERTY_KEY, TEST_PROPERTY_VALUE)
        performTest()
    }

    @Test
    void credentialsOnlyAddedToMavenRepositories() {
        withMavenSettings {
            servers.add new Server(id: 'flat', username: TEST_USER_NAME, password: TEST_USER_PASSWORD)
        }

        withDependencyRepositoriesBlock("""
                flatDir {
                    name = 'flat'
                    dirs '.'
                }
        """)
        expectEffectiveDependencyRepositories([new RepositoryBean('flat', 'dir')])
        performTest()
    }

    @Test
    void credentialsAddedToMavenDependencyAndPluginManagamentRepository() {
        withMavenSettings {
            servers.add new Server(id: 'central', username: TEST_USER_NAME, password: TEST_USER_PASSWORD)
        }

        withPluginManagementRepositoriesBlock("""
            maven {
                name = 'central'
                url = 'https://repo1.maven.org/maven2/'
            }
        """)
        withDependencyRepositoriesBlock("""
            maven {
                name = 'central'
                url = 'https://repo1.maven.org/maven2/'
            }
        """)
        RepositoryBean repoBean = new RepositoryBean('central', 'https://repo1.maven.org/maven2/')
        expectEffectivePluginManagementRepositories([repoBean])
        expectEffectiveDependencyRepositories([repoBean])
        expectCredentials([new RepoCredentials('Effective pluginManagement', 'central', TEST_USER_NAME, TEST_USER_PASSWORD)])
        expectCredentials([new RepoCredentials('Effective dependency', 'central', TEST_USER_NAME, TEST_USER_PASSWORD)])
        performTest()
    }

    @Test
    void replaceMavenLocalURLWithConfigFromSettingsFile() {
        withMavenSettings {
            localRepository = getMavenLocalTestDirName()
        }

        withDependencyRepositoriesBlock("""
            mavenLocal()
        """)
        expectEffectiveDependencyRepositories([new RepositoryBean(GradleConstants.GRADLE_MAVEN_LOCAL_REPO_NAME, getMavenLocalTestDir().toURI().toString())])
        performTest(true)
    }

}
