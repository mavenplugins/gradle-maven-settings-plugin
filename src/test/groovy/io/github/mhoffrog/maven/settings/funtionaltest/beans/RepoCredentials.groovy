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

package io.github.mhoffrog.maven.settings.funtionaltest.beans

class RepoCredentials {

    final String repoContext
    final String repoName
    final String username
    final String password

    RepoCredentials(String repoContext, String repoName, String username, String password) {
        this.repoContext = repoContext
        this.repoName = repoName
        this.username = username
        this.password = password
    }
}
