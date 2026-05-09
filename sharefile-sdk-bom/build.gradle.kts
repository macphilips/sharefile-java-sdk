plugins {
    `java-platform`
    `maven-publish`
    signing
}

group = rootProject.group
version = rootProject.version

dependencies {
    constraints {
        api(project(":sharefile-sdk-core"))
        api(project(":sharefile-sdk-client"))
        api(project(":sharefile-spring-boot-starter"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["javaPlatform"])
            pom {
                name.set(project.name)
                description.set("ShareFile REST API SDK for Java - BOM")
                url.set("https://github.com/indraftapp/sharefile-java-sdk")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("indraftapp")
                        name.set("Indraft")
                    }
                }
                scm {
                    url.set("https://github.com/indraftapp/sharefile-java-sdk")
                    connection.set("scm:git:git://github.com/indraftapp/sharefile-java-sdk.git")
                    developerConnection.set("scm:git:ssh://github.com/indraftapp/sharefile-java-sdk.git")
                }
            }
        }
    }
}

signing {
    val signingKey = providers.environmentVariable("GPG_SIGNING_KEY")
    val signingPassword = providers.environmentVariable("GPG_SIGNING_PASSWORD")
    if (signingKey.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassword.get())
    }
    sign(publishing.publications["mavenJava"])
}

tasks.withType<Sign>().configureEach {
    onlyIf { !version.toString().endsWith("-SNAPSHOT") }
}
