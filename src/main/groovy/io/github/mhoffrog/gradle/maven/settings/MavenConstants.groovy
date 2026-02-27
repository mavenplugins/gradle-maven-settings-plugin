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

package io.github.mhoffrog.gradle.maven.settings

class MavenConstants {

    // According to {@link RepositorySystem#defaultUserLocalRepository}, the default local repository is located at ${user.home}/.m2/repository
    static final File DEFAULT_M2_HOME_DIR = new File(System.getProperty("user.home"), ".m2")

    static final File DEFAULT_MAVEN_SETTINGS_FILE = new File(DEFAULT_M2_HOME_DIR, "settings.xml")

    static final String DEFAULT_MAVEN_SETTINGS_FILE_NAME = DEFAULT_MAVEN_SETTINGS_FILE.canonicalPath

    static final URI DEFAULT_MAVEN_LOCAL_REPO_URI = new File(DEFAULT_M2_HOME_DIR, "repository").toURI()

}
