plugins {
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(providers.environmentVariable("OSSRH_USERNAME"))
            password.set(providers.environmentVariable("OSSRH_PASSWORD"))
        }
    }
}

subprojects {
    // BOM uses java-platform with its own publishing config — skip here
    if (name == "sharefile-sdk-bom") return@subprojects

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                pom {
                    name.set(project.name)
                    description.set("ShareFile REST API SDK for Java")
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

    configure<SigningExtension> {
        val signingKey = providers.environmentVariable("GPG_SIGNING_KEY")
        val signingPassword = providers.environmentVariable("GPG_SIGNING_PASSWORD")
        if (signingKey.isPresent) {
            useInMemoryPgpKeys(signingKey.get(), signingPassword.get())
        }
        sign(the<PublishingExtension>().publications["mavenJava"])
    }

    tasks.withType<Sign>().configureEach {
        onlyIf { !version.toString().endsWith("-SNAPSHOT") }
    }
}
