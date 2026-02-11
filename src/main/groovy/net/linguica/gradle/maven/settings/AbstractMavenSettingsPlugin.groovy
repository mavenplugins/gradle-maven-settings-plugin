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

import groovy.transform.CompileStatic
import io.github.mhoffrog.gradle.scope.IGradlePluginScopeUtilizer
import org.apache.maven.model.Profile
import org.apache.maven.model.building.ModelProblemCollector
import org.apache.maven.model.building.ModelProblemCollectorRequest
import org.apache.maven.model.path.DefaultPathTranslator
import org.apache.maven.model.path.ProfileActivationFilePathInterpolator
import org.apache.maven.model.profile.DefaultProfileActivationContext
import org.apache.maven.model.profile.DefaultProfileSelector
import org.apache.maven.model.profile.activation.*
import org.apache.maven.settings.Mirror
import org.apache.maven.settings.Server
import org.apache.maven.settings.SettingsUtils
import org.apache.maven.settings.building.SettingsBuildingException
import org.gradle.api.GradleScriptException
import org.gradle.api.artifacts.ArtifactRepositoryContainer
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.ExtraPropertiesExtension

import javax.annotation.Nullable
import java.util.Map.Entry

@CompileStatic
abstract class AbstractMavenSettingsPlugin {
    public static final String MAVEN_SETTINGS_EXTENSION_NAME = "mavenSettings"

    private static final String LOG_PREFIX = "MavenSettingsPlugin:";

    private org.apache.maven.settings.Settings mavenSettings

    private IGradlePluginScopeUtilizer scopeUtilizer

    private Logger logger;

    void apply(IGradlePluginScopeUtilizer scopeUtilizer) {
        this.scopeUtilizer = scopeUtilizer
        logger = scopeUtilizer.logger
        logger.info("${LOG_PREFIX} Applying Maven Settings to Gradle ${scopeUtilizer.settingsScope ? "Settings" : "Project"} scope.")

        MavenSettingsPluginExtension extension =
                scopeUtilizer.extensions.create(MAVEN_SETTINGS_EXTENSION_NAME, MavenSettingsPluginExtension.class, scopeUtilizer)

        scopeUtilizer.setPluginEventClosure {
            loadSettings(extension)
            activateProfiles(scopeUtilizer, extension)
            if (!scopeUtilizer.repositoriesConfigured
                    && !scopeUtilizer.pluginManagementRepositoriesConfigured
                    && !scopeUtilizer.pushRepositoriesConfigured) {
                logger.warn("${LOG_PREFIX} No repositories configured in this scope. No repo credentials to apply.")
            } else {
                registerMirrors(scopeUtilizer.repositories)
                registerMirrors(scopeUtilizer.pluginManagementRepositories)
                applyRepoCredentials('PluginManagement', scopeUtilizer.pluginManagementRepositories)
                applyRepoCredentials('DependencyResolution', scopeUtilizer.repositories)
                applyRepoCredentials('Publishing', scopeUtilizer.pushRepositories)
            }
        }
    }

    private void loadSettings(MavenSettingsPluginExtension extension) {
        LocalMavenSettingsLoader settingsLoader = new LocalMavenSettingsLoader(extension)
        try {
            mavenSettings = settingsLoader.loadSettings()
        } catch (SettingsBuildingException e) {
            throw new GradleScriptException("${LOG_PREFIX} Unable to read local Maven settings.", e)
        }
    }

    private void activateProfiles(IGradlePluginScopeUtilizer scopeUtilizer, MavenSettingsPluginExtension extension) {
        DefaultProfileSelector profileSelector = new DefaultProfileSelector()
        DefaultProfileActivationContext activationContext = new DefaultProfileActivationContext()
        List<ProfileActivator> profileActivators = [new JdkVersionProfileActivator(),
                                                    new OperatingSystemProfileActivator(),
                                                    new PropertyProfileActivator(),
                                                    new FileProfileActivator()
                                                            .setProfileActivationFilePathInterpolator(new ProfileActivationFilePathInterpolator().setPathTranslator(new DefaultPathTranslator()))
        ]
        profileActivators.each { profileSelector.addProfileActivator(it) }

        activationContext.setActiveProfileIds(extension.activeProfiles.toList() + mavenSettings.activeProfiles)
        activationContext.setProjectDirectory(scopeUtilizer.projectDir)
        activationContext.setSystemProperties(System.getProperties())
        if (extension.exportGradleProps) {
            Map properties = scopeUtilizer.properties
            activationContext.setUserProperties(properties.collectEntries { key, value -> [key, value.toString()] } as Map<String, String>)
        }

        List<Profile> profiles = profileSelector.getActiveProfiles(mavenSettings.profiles.collect { return SettingsUtils.convertFromSettingsProfile(it) },
                activationContext, new ModelProblemCollector() {
            @Override
            void add(ModelProblemCollectorRequest req) {

            }
        })

        profiles.each { profile ->
            for (Entry entry : profile.properties) {
                ExtensionContainer extensions = scopeUtilizer.extensions
                extensions.getByType(ExtraPropertiesExtension).set(entry.key.toString(), entry.value.toString())
            }
        }
    }

    private void registerMirrors(final RepositoryHandler repositories) {
        Mirror globalMirror = mavenSettings.mirrors.find { it.mirrorOf.split(',').contains('*') }
        if (globalMirror != null) {
            logger.info("${LOG_PREFIX} Found global mirror in settings.xml. Replacing Maven repositories with mirror " +
                    "located at ${globalMirror.url}")
            createMirrorRepository(repositories, globalMirror)
            return
        }

        Mirror externalMirror = mavenSettings.mirrors.find { it.mirrorOf.split(',').contains('external:*') }
        if (externalMirror != null) {
            logger.info("${LOG_PREFIX} Found external mirror in settings.xml. Replacing non-local Maven repositories " +
                    "with mirror located at ${externalMirror.url}.")
            createMirrorRepository(repositories, externalMirror) { MavenArtifactRepository repo ->
                InetAddress host = InetAddress.getByName(repo.url.host)
                // only match repositories not on localhost and not file based
                repo.url.scheme != 'file' && !(host.anyLocalAddress || host.isLoopbackAddress() || NetworkInterface.getByInetAddress(host) != null)
            }
            return
        }

        Mirror centralMirror = mavenSettings.mirrors.find { it.mirrorOf.split(',').contains('central') }
        if (centralMirror != null) {
            logger.info("${LOG_PREFIX} Found central mirror in settings.xml. Replacing Maven Central repository with " +
                    "mirror located at ${centralMirror.url}.")
            createMirrorRepository(repositories, centralMirror) { MavenArtifactRepository repo ->
                ArtifactRepositoryContainer.MAVEN_CENTRAL_URL.startsWith(repo.url.toString())
            }
        }
    }

    private void applyRepoCredentials(String repoContext, @Nullable RepositoryHandler repositories) {
        repositories?.all { repo ->
            if (repo instanceof MavenArtifactRepository) {
                logger.info("${LOG_PREFIX} ${repoContext} repository '${repo.name}' '${repo.url}' found.")
                mavenSettings.servers.each { server ->
                    if (repo.name == server.id) {
                        addCredentials(logger, server, repo as MavenArtifactRepository)
                    }
                }
            }
        }
    }

    private void createMirrorRepository(RepositoryHandler repositories, Mirror mirror) {
        createMirrorRepository(repositories, mirror) { true }
    }

    private void createMirrorRepository(RepositoryHandler repositories, Mirror mirror, Closure<Boolean> predicate) {
        boolean mirrorFound = false
        List<String> excludedRepositoryNames = mirror.mirrorOf.split(',').findAll { it.startsWith("!") }.collect { it.substring(1) }
        repositories?.all { repo ->
            if (repo instanceof MavenArtifactRepository && repo.name != ArtifactRepositoryContainer.DEFAULT_MAVEN_LOCAL_REPO_NAME
                    && repo.url != URI.create(mirror.url) && predicate(repo) && !excludedRepositoryNames.contains(repo.getName())) {
                repositories.remove(repo)
                mirrorFound = true
            }
        }

        if (mirrorFound) {
            Server server = mavenSettings.getServer(mirror.id)
            repositories.maven { MavenArtifactRepository repo ->
                repo.name = mirror.name ?: mirror.id
                repo.url = mirror.url
                addCredentials(logger, server, repo)
            }
        }
    }

    private static addCredentials(Logger logger, Server server, MavenArtifactRepository repo) {
        if (server?.username != null && server?.password != null) {
            repo.credentials {
                it.username = server.username
                it.password = server.password
            }
            logger.info("${LOG_PREFIX} Applying credentials for '${repo.url}' from settings.xml server '${server.id}'.")
        }
    }
}
