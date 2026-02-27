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

    private static String LOG_PREFIX_TEMPLATE

    private static final String LOG_PREFIX_SCOPE_TOKEN = '@SCOPE@'

    private String logPrefix

    private org.apache.maven.settings.Settings mavenSettings

    private IGradlePluginScopeUtilizer scopeUtilizer

    private Logger logger

    private Mirror globalMirror

    private Mirror externalMirror

    private Mirror centralMirror

    static {
        final Properties properties = new Properties()
        final String propertiesResourcePath = '/maven-settings-plugin-expanded.properties'
        final URL propsUrl = AbstractMavenSettingsPlugin.class.getResource(propertiesResourcePath)
        if (propsUrl == null) {
            throw new IllegalStateException("Resource '${propertiesResourcePath}' not found on classpath.")
        }
        propsUrl.withInputStream {
            properties.load(it)
        }
        LOG_PREFIX_TEMPLATE = "MavenSettings[${LOG_PREFIX_SCOPE_TOKEN}] v${properties['plugin.version']}:"
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
                determineMirrors()
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

    private void determineMirrors() {
        globalMirror = mavenSettings.mirrors.find { it.mirrorOf.split(',').collect { token -> token.trim() }.contains('*') }
        if (globalMirror != null) {
            logger.info("${logPrefix} Found global mirror in settings.xml: '${resolveMirrorName(globalMirror)}' at '${globalMirror.url}'.")
        }
        externalMirror = mavenSettings.mirrors.find { it.mirrorOf.split(',').collect { token -> token.trim() }.contains('external:*') }
        if (externalMirror != null) {
            logger.info("${logPrefix} Found external mirror in settings.xml: '${resolveMirrorName(externalMirror)}' at '${externalMirror.url}'.")
        }
        centralMirror = mavenSettings.mirrors.find { it.mirrorOf.split(',').collect { token -> token.trim() }.contains('central') }
        if (centralMirror != null) {
            logger.info("${logPrefix} Found central mirror in settings.xml:  '${resolveMirrorName(centralMirror)}' at '${centralMirror.url}'.")
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

    private void registerMirrors(final MavenSettingsPluginExtension extension, final RepositoryHandler repositories) {
        if (globalMirror != null) {
            createMirrorRepository(extension, repositories, globalMirror)
            return
        }

        if (externalMirror != null) {
            createMirrorRepository(extension, repositories, externalMirror) { MavenArtifactRepository repo ->
                // only match repositories not on localhost and not file based
                if (repo.url.scheme == 'file') {
                    return false
                }
                try {
                    InetAddress host = InetAddress.getByName(repo.url.host)
                    return !(host.anyLocalAddress || host.isLoopbackAddress() || NetworkInterface.getByInetAddress(host) != null)
                } catch (UnknownHostException ignored) {
                    // Cannot resolve hostname - treat as external
                    return true
                }
            }
            return
        }

        if (centralMirror != null) {
            createMirrorRepository(extension, repositories, centralMirror) { MavenArtifactRepository repo ->
                ArtifactRepositoryContainer.MAVEN_CENTRAL_URL.startsWith(repo.url.toString())
            }
        }
    }

    private void applyRepoCredentials(String repoContext, @Nullable RepositoryHandler repositories) {
        repositories?.all { repo ->
            if (repo instanceof MavenArtifactRepository) {
                logger.info("${logPrefix} ${repoContext} repository '${repo.name}' '${repo.url}' found.")
                mavenSettings.servers.each { server ->
                    if (repo.name == server.id) {
                        addCredentials(logger, server, repo as MavenArtifactRepository)
                    }
                }
            }
        }
    }

    private void createMirrorRepository(MavenSettingsPluginExtension extension, RepositoryHandler repositories, Mirror mirror) {
        createMirrorRepository(extension, repositories, mirror) { true }
    }

    private void createMirrorRepository(MavenSettingsPluginExtension extension, RepositoryHandler repositories, Mirror mirror, Closure<Boolean> predicate) {
        List<String> excludedRepositoryNames = mirror.mirrorOf.split(',').collect { it.trim() }.findAll { it.startsWith("!") }.collect { it.substring(1) }
        List toRemove = []
        repositories?.each { repo ->
            if (repo instanceof MavenArtifactRepository && repo.url != URI.create(mirror.url) && predicate(repo)) {
                if (excludedRepositoryNames.contains(repo.name)) {
                    logger.info("${logPrefix} Repository '${repo.name}' '${repo.url}' is excluded from mirror application by mirrorOf '${mirror.mirrorOf}'. Skipping.")
                } else if (extension.mirrorExclusions.contains(repo.name)) {
                    logger.info("${logPrefix} Repository '${repo.name}' '${repo.url}' is excluded from mirror application by plugin configuration mirrorExclusions='${extension.mirrorExclusions.join(",")}'. Skipping.")
                } else {
                    logger.info("${logPrefix} Repository '${repo.name}' '${repo.url}' is being replaced by mirror located at ${mirror.url} according to mirrorOf '${mirror.mirrorOf}'.")
                    toRemove.add(repo)
                }
            }
        }

        if (!toRemove.empty) {
            toRemove.each { repositories.remove(it) }
            Server server = mavenSettings.getServer(mirror.id)
            repositories.maven { MavenArtifactRepository repo ->
                repo.name = resolveMirrorName(mirror)
                repo.url = mirror.url
                addCredentials(logger, server, repo)
            }
        }
    }

    private String resolveMirrorName(Mirror mirror) {
        return mirror.name ?: mirror.id
    }

    private addCredentials(Logger logger, Server server, MavenArtifactRepository repo) {
        if (server?.username != null && server?.password != null) {
            repo.credentials {
                it.username = server.username
                it.password = server.password
            }
            logger.info("${logPrefix} Applying credentials for '${repo.url}' from settings.xml server '${server.id}'.")
        }
    }

}
