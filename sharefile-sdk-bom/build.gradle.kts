plugins {
    `java-platform`
    `maven-publish`
}

val isPublishingToMavenLocal =
    gradle.startParameter.taskNames.any { requestedTask ->
        requestedTask == "publishToMavenLocal" || requestedTask.endsWith(":publishToMavenLocal")
    }

if (!isPublishingToMavenLocal) {
    apply(plugin = "signing")
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
                url.set("https://github.com/macphilips/sharefile-java-sdk")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("macphilips")
                        name.set("Titilope Philips")
                    }
                }
                scm {
                    url.set("https://github.com/macphilips/sharefile-java-sdk")
                    connection.set("scm:git:git://github.com/macphilips/sharefile-java-sdk.git")
                    developerConnection.set("scm:git:ssh://github.com/macphilips/sharefile-java-sdk.git")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/macphilips/sharefile-java-sdk")
            credentials {
                username =
                    providers.environmentVariable("GITHUB_ACTOR").orElse(
                        providers.environmentVariable("GITHUB_PACKAGES_USERNAME")
                    ).orNull
                password =
                    providers.environmentVariable("GITHUB_TOKEN").orElse(
                        providers.environmentVariable("GITHUB_PACKAGES_TOKEN")
                    ).orNull
            }
        }
    }
}

if (!isPublishingToMavenLocal) {
    configure<SigningExtension> {
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
}
