# Gradle Maven settings plugin

[![Apache License](https://img.shields.io/github/license/mavenplugins/gradle-maven-settings-plugin?label=License)](./LICENSE.txt)
[![CI](https://github.com/mavenplugins/gradle-maven-settings-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/mavenplugins/gradle-maven-settings-plugin/actions/workflows/build.yml)
[![Maven Central Snapshot](https://img.shields.io/maven-metadata/v?label=Maven%20Central%20Snapshot&metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fio%2Fgithub%2Fmavenplugins%2Fgradle%2Fmaven-settings%2Fmaven-metadata.xml)](https://central.sonatype.com/repository/maven-snapshots/io/github/mavenplugins/gradle/maven-settings/maven-metadata.xml)
[![Maven Central Release](https://img.shields.io/maven-central/v/io.github.mavenplugins.gradle/maven-settings?label=Maven%20Central%20Release)](https://search.maven.org/artifact/io.github.mavenplugins.gradle/maven-settings)

This Gradle plugin provides a migration path for projects coming from a Maven ecosystem.
It exposes standard Maven configurations located in [settings files](http://maven.apache.org/settings.html) to your
Gradle project.

The plugin has been originally forked from https://github.com/rmanibus/gradle-maven-settings-plugin.

## Enhanced Features

- Plugin can be applied to Settings and Project scope. This includes support for mirror exclusions and profile
  activation.
- Expose properties defined in Maven settings files to the Gradle scope.
- Add `repository` and `pluginRepository` definitions from settings files to the Gradle scope.
- Apply repository credentials defined in Maven settings files to Gradle:
    - DependencyResolution repositories - Settings and Project scope.
    - PluginManagement repositories - Settings scope only.
    - Publishing repositories - Settings and Project scope.
- Exclude Gradle built-in repository names `Gradle Central Plugin Repository`, `Google` and `MavenLocal` from mirror
  application by default to prevent accidental overriding of important repositories. This list is configurable via the
  `mirrorExclusions` property.
- Apply `localRepository` from a `userSettingsFileName` configured settings file to the Gradle `MavenLocal` repo if
  `localRepository` is set in there. (Since plugin version `1.1.0`).

### Log Output

- Each plugin action is logged with the effective scope reference on info level for better visibility into the plugin's
  configuration modifications.

### Unit Tests

- Tests migrated to JUnit 5
- Functional tests added for the Gradle Settings scope.

### Compatibility

- Gradle: The plugin is compatible with Gradle `2.0` and later. It has been tested with Gradle `9.4.1`.
- Java: The plugin is compatible with `Java 8` and later.
- Maven: The plugin is compatible with Maven `3.x` settings files. It has dependencies on Maven `3.9.14`.

## Usage

<!-- This plugin is hosted on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/io.github.mavenplugins.maven-settings). -->
This plugin is hosted
on [Maven Central](https://repo1.maven.org/maven2/io/github/mavenplugins/gradle/maven-settings).
To use the plugin, add the following to your `build.gradle` file.

    plugins {
      id 'io.github.mavenplugins.gradle.maven-settings' version '1.1.0'
    }

For Gradle 2.0 or earlier you must add the following:

    buildscript {
        repositories {
            mavenCentral()
        }
        
        dependencies {
            classpath 'io.github.mavenplugins.gradle:maven-settings:1.1.0'
        }
    }

    apply plugin: 'io.github.mavenplugins.gradle.maven-settings'

## Mirrors

The plugin exposes Maven-like mirror capabilities. The plugin will properly register and enforce any
mirrors defined in a `settings.xml` with `<mirrorOf>` values of `*`, `external:*` or `central`. Existing
`repositories {...}` definitions that match these identifiers will be removed.

Exclusions are also taken into consideration. For example, a `<mirrorOf>` value of `*,!myRepo` will replace
all repositories except for the repository with the name 'myRepo'.

## Credentials

The plugin will attempt to apply credentials located in `<server>` elements to appropriate Maven repository
definitions in your build script. This is done by matching the `<id>` element in the `settings.xml` file to the `name`
property of the repository definition.

In a `settings.gradle` file:

    dependencyResolutionManagement {
        repositories {
            maven {
                name = 'myRepo' // should match <id>myRepo</id> of appropriate <server> in settings.xml
                url = 'https://intranet.foo.org/repo'
            }
        }
    }

In a `build.gradle` file:

    repositories {
        maven {
            name = 'myRepo' // should match <id>myRepo</id> of appropriate <server> in settings.xml
            url = 'https://intranet.foo.org/repo'
        }
    }

Server credentials are used for mirrors as well. When mirrors are added the plugin will look for a `<server>` element
with the same `<id>` and the configured credentials are used
and [decrypted](http://maven.apache.org/guides/mini/guide-encryption.html)
if necessary.

### Publishing

The plugin will also attempt to apply credentials to repositories configured using the
['maven-publish'](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin.

    publishing {
        repositories {
            maven {
                name = 'myRepo' // should match <id>myRepo</id> of appropriate <server> in settings.xml
                url = 'https://intranet.foo.org/repo/repositories/releases'
            }
        }
    }

> **Note:** Currently only Basic Authentication using username and password is supported at this time.

## Profiles

Profiles defined in a `settings.xml` will have their properties exported to the Gradle project when the profile is
considered active. Active profiles are those listed in the `<activeProfiles>` section of the `settings.xml`, the
`activeProfiles`
property of the `mavenSettings {...}` configuration closure, or those that satisfy the given profile's `<activation>`
criteria.

### Repositories

Repositories defined within active profiles will be added to the Gradle scope and mirror definitions will be applied to
them as well. Plugin repositories defined within active profiles will be added to the Gradle Settings scope
pluginManagement repositories. The latter must be enabled explicitly via the `addPluginRepositories` property of the
`mavenSettings {...}` configuration closure.

## Configuration

Configuration of the Maven settings plugin is done via the `mavenSettings {...}` configuration closure. The following
properties are available.

* `userSettingsFileName` - String representing the path of the file to be used as the user settings file. This defaults
  to
  `'$USER_HOME/.m2/settings.xml'`
* `activeProfiles` - List of profile ids to treat as active.
* `exportGradleProps` - Flag indicating whether or not Gradle project properties should be exported for the purposes of
  settings file property interpolation and profile activation. This defaults to `true`.
* `addRepositories` - Flag indicating whether or not repositories defined in the settings file should be added to the
  Gradle scope. This defaults to `true`.
* `addPluginRepositories` - Flag indicating whether or not plugin repositories defined in the settings file should be
  added to the Gradle Settings scope pluginManagement repositories. This defaults to `false`.
* `mirrorExclusions` - List of repository names to be excluded from applying a mirror definition from settings.xml.
  This defaults to `['Gradle Central Plugin Repository', 'Google', 'MavenLocal', 'MavenCentralSnapshots']`.
* `defaultMirrorExclusions` - Read-only property representing the default list of repository names to be excluded from
  applying a mirror definition from settings.xml.
  This is `['Gradle Central Plugin Repository', 'Google', 'MavenLocal', 'MavenCentralSnapshots']` and is intended to be
  used as a base when extending `mirrorExclusions` to preserve default exclusions.
