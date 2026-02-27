package io.github.mhoffrog.maven.settings.funtionaltest.beans

class RepositoryBean {
    final String name
    final String url

    RepositoryBean(String name, String url) {
        this.name = name
        this.url = url
    }
}
