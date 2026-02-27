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

class GradleConstants {

    // {@link DefaultRepositoryHandler#DEFAULT_MAVEN_LOCAL_REPO_NAME} - since Gradle 4.8 to < Gradle 6.0
    // {@link ArtifactRepositoryContainer#DEFAULT_MAVEN_LOCAL_REPO_NAME} - since Gradle 6.0
    static final String GRADLE_MAVEN_LOCAL_REPO_NAME = 'MavenLocal'

    // {@link DefaultRepositoryHandler#DEFAULT_MAVEN_CENTRAL_REPO_NAME} - since Gradle 4.8 to < Gradle 6.0
    // {@link ArtifactRepositoryContainer#DEFAULT_MAVEN_CENTRAL_REPO_NAME} - since Gradle 6.0
    static final String GRADLE_MAVEN_CENTRAL_REPO_NAME = 'MavenRepo'

    // {@link DefaultRepositoryHandler#GRADLE_PLUGIN_PORTAL_REPO_NAME} - since Gradle 4.8
    static final String GRADLE_PLUGIN_PORTAL_REPO_NAME = 'Gradle Central Plugin Repository'

    // {@link DefaultRepositoryHandler#GOOGLE_REPO_NAME} - since Gradle 4.8
    static final String GOOGLE_REPO_NAME = 'Google'

}
