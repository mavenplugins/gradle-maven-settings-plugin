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
import io.github.mhoffrog.gradle.maven.settings.scope.IGradlePluginScopeUtilizer
import io.github.mhoffrog.gradle.maven.settings.utils.PluginResourcesUtil
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

    private static final String LOG_PREFIX_TEMPLATE

    private static final String LOG_PREFIX_SCOPE_TOKEN = '@SCOPE@'

    private String logPrefix

    private org.apache.maven.settings.Settings mavenSettings

    private IGradlePluginScopeUtilizer scopeUtilizer

    private Logger logger

    static {
        LOG_PREFIX_TEMPLATE = "${PluginResourcesUtil.pluginNameInLogPrefix}[${LOG_PREFIX_SCOPE_TOKEN}] v${PluginResourcesUtil.pluginVersion}:"
    }

    void apply(IGradlePluginScopeUtilizer scopeUtilizerParam) {
        scopeUtilizer = scopeUtilizerParam
        logPrefix = LOG_PREFIX_TEMPLATE.replace(LOG_PREFIX_SCOPE_TOKEN, scopeUtilizer.scopeName)
        logger = scopeUtilizer.logger
        logger.info("${logPrefix} Applying Maven Settings to Gradle ${scopeUtilizer.scopeName} scope.")

        final MavenSettingsPluginExtension extension =
                this.scopeUtilizer.extensions.create(MAVEN_SETTINGS_EXTENSION_NAME, MavenSettingsPluginExtension.class, scopeUtilizer)

        scopeUtilizer.setPluginEventClosure {
            loadSettings(extension)
            activateProfiles(scopeUtilizer, extension)
            if (!scopeUtilizer.repositoriesConfigured
                    && !scopeUtilizer.pluginManagementRepositoriesConfigured
                    && !scopeUtilizer.publishingRepositoriesConfigured) {
                logger.warn("${logPrefix} No repositories configured in ${scopeUtilizer.scopeName} scope. No repo credentials to apply.")
            } else {
                if (extension.isNonDefaultUserSettingsFileConfigured()) {
                    [
                            scopeUtilizer.pluginManagementRepositories,
                            scopeUtilizer.repositories,
                            scopeUtilizer.publishingRepositories
                    ].each { RepositoryHandler repos ->
                        applyMavenLocalFromUserSettingsFileConfigured(extension, repos)
                    }
                }
                registerMirrors(extension, scopeUtilizer.pluginManagementRepositories)
                registerMirrors(extension, scopeUtilizer.repositories)
                applyRepoCredentials('PluginManagement', scopeUtilizer.pluginManagementRepositories)
                applyRepoCredentials('DependencyResolution', scopeUtilizer.repositories)
                applyRepoCredentials('Publishing', scopeUtilizer.publishingRepositories)
            }
        }
    }

    private void loadSettings(MavenSettingsPluginExtension extension) {
        logger.info("${logPrefix} Loading Maven user settings from file '${extension.userSettingsFile}'.")
        LocalMavenSettingsLoader settingsLoader = new LocalMavenSettingsLoader(extension)
        try {
            mavenSettings = settingsLoader.loadSettings()
        } catch (SettingsBuildingException e) {
            throw new GradleScriptException("${logPrefix} Unable to read local Maven settings.", e)
        }
        if (mavenSettings.localRepository) {
            logger.info("${logPrefix} localRepository is defined in Maven settings file as: ${mavenSettings.localRepository}")
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
            activationContext.setUserProperties(properties.findAll { key, value -> value != null }.collectEntries { key, value -> [key, value.toString()] } as Map<String, String>)
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

    private void applyMavenLocalFromUserSettingsFileConfigured(final MavenSettingsPluginExtension extension, final RepositoryHandler repositories) {
        repositories?.all { repo ->
            if (repo instanceof MavenArtifactRepository
                    && repo.name == ArtifactRepositoryContainer.DEFAULT_MAVEN_LOCAL_REPO_NAME) {
                URI mavenSettingsLocalRepoURI
                // mavenSettingsLocalRepoURI = mavenSettings.localRepository ?
                //     new File(mavenSettings.localRepository).toURI()
                if (mavenSettings.localRepository) {
                    mavenSettingsLocalRepoURI = new File(mavenSettings.localRepository).toURI()
                    if (repo.url != mavenSettingsLocalRepoURI) {
                        logger.info("${logPrefix} Replacing ${repo.name} repository with URL ${mavenSettingsLocalRepoURI}.")
                    }
                } else {
                    // For now we do not enforce Maven default behavior as applied for an original user settings.xml
                    // without localRepository element, which is to use ${user.home}/.m2/repository as the local repo location.
                    // This is because it may be unexpected for users of the plugin that do specify a custom settings file
                    // without a localRepository element that their mavenLocal() repository URL gets changed to a default location
                    // that may not even exist on their system. Instead, we log a warning and leave the mavenLocal() repository URL unchanged,
                    // which also allows users to still use mavenLocal() with a custom settings file without a localRepository element if they want to.
                    //mavenSettingsLocalRepoURI = MavenConstants.DEFAULT_MAVEN_LOCAL_REPO_URI
                    logger.warn("${logPrefix} No localRepository element found in ${extension.userSettingsFileName}. Leaving ${repo.name} unchanged with URL ${repo.url}.")
                }
                if (mavenSettingsLocalRepoURI) {
                    repo.url = mavenSettingsLocalRepoURI
                }
            }
        }
    }

    private void registerMirrors(MavenSettingsPluginExtension extension, RepositoryHandler repositories) {
        List<MavenArtifactRepository> reposToRemove = []
        Map<String, Mirror> mirrorsToAdd = new HashMap<>()
        repositories?.each { repo ->
            if (repo instanceof MavenArtifactRepository) {
                if (extension.mirrorExclusions.contains(repo.name)) {
                    logger.info("${logPrefix} Repository '${repo.name}' '${repo.url}' is excluded from mirror application by plugin configuration mirrorExclusions='${extension.mirrorExclusions.join(",")}'. Skipping.")
                } else {
                    Mirror mirror = findMirrorMatchingForRepo(repo)
                    if (mirror) {
                        logger.info("${logPrefix} Repository '${repo.name}' '${repo.url}' is being replaced by mirror located at ${mirror.url} according to mirrorOf '${mirror.mirrorOf}'.")
                        mirrorsToAdd.put(mirror.id, mirror)
                        reposToRemove.add(repo)
                    }
                }
            }
        }

        reposToRemove.each { repositories.remove(it) }

        mirrorsToAdd.values().each { mirror ->
            repositories.maven { MavenArtifactRepository repo ->
                repo.name = mirror.id
                repo.url = mirror.url
            }
        }
    }

    private Mirror findMirrorMatchingForRepo(final MavenArtifactRepository repo) {
        for (Mirror mirror : mavenSettings.mirrors) {
            Mirror mirrorFound = null
            boolean isBreakMirrorOf = false
            for (String mirrorOf : mirror.mirrorOf.split(',').collect { it.trim() }) {
                switch (mirrorOf) {
                    case '*':
                        mirrorFound = mirror
                        break
                    case 'external:*':
                        if (repo.url.scheme != 'file') {
                            String host = repo.url.host
                            boolean isLocalHost = !host ||
                                    host.equalsIgnoreCase('localhost') ||
                                    host == '127.0.0.1' ||
                                    host == '::1'
                            if (!isLocalHost) {
                                mirrorFound = mirror
                            }
                        }
                        break
                    case 'central':
                        if (repo.url.toString().startsWith(ArtifactRepositoryContainer.MAVEN_CENTRAL_URL)) {
                            mirrorFound = mirror
                        }
                        break
                    default:
                        if (mirrorOf == repo.name) {
                            mirrorFound = mirror
                            isBreakMirrorOf = true
                        } else if (mirrorOf == "!${repo.name}") {
                            logger.info("${logPrefix} Repository '${repo.name}' '${repo.url}' is excluded from mirror '${resolveMirrorName(mirror)}' at '${mirror.url}' by mirrorOf '!${repo.name}'. Skipping.")
                            mirrorFound = null
                            isBreakMirrorOf = true
                        }
                }
                if (isBreakMirrorOf) {
                    break
                }
            }
            if (mirrorFound) {
                logger.info("${logPrefix} Repository '${repo.name}' '${repo.url}' matches mirrorOf '${mirrorFound.mirrorOf}' in mirror '${resolveMirrorName(mirrorFound)}' at '${mirrorFound.url}'.")
                return mirrorFound
            }
        }
        return null
    }

    private void applyRepoCredentials(String repoContext, @Nullable RepositoryHandler repositories) {
        repositories?.each { repo ->
            if (repo instanceof MavenArtifactRepository) {
                logger.info("${logPrefix} ${repoContext} repository '${repo.name}' '${repo.url}' found.")
                mavenSettings.servers.each { server ->
                    if (repo.name == server.id) {
                        addCredentials(server, repo)
                    }
                }
            }
        }
    }

    private String resolveMirrorName(Mirror mirror) {
        return mirror.name ?: mirror.id
    }

    private addCredentials(Server server, MavenArtifactRepository repo) {
        if (server?.username != null && server?.password != null) {
            repo.credentials {
                it.username = server.username
                it.password = server.password
            }
            logger.info("${logPrefix} Applying credentials for '${repo.url}' from settings.xml server '${server.id}'.")
        }
    }

}
