import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.w3c.dom.Element
import java.math.BigDecimal
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    base
    jacoco
    id("io.github.indraftapp.sharefile.root-style")
    id("io.github.indraftapp.sharefile.git-hooks")
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

val isPublishingToMavenLocal =
    gradle.startParameter.taskNames.any { requestedTask ->
        requestedTask == "publishToMavenLocal" || requestedTask.endsWith(":publishToMavenLocal")
    }

repositories {
    mavenCentral()
}

jacoco {
    toolVersion = "0.8.13"
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
    apply(plugin = "io.github.indraftapp.sharefile.java-checkstyle")
    apply(plugin = "jacoco")
    apply(plugin = "maven-publish")
    if (!isPublishingToMavenLocal) {
        apply(plugin = "signing")
    }

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    dependencies {
        add("compileOnly", "org.projectlombok:lombok:1.18.46")
        add("annotationProcessor", "org.projectlombok:lombok:1.18.46")
        add("testCompileOnly", "org.projectlombok:lombok:1.18.46")
        add("testAnnotationProcessor", "org.projectlombok:lombok:1.18.46")
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

    configure<JacocoPluginExtension> {
        toolVersion = rootProject.extensions.getByType<JacocoPluginExtension>().toolVersion
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                pom {
                    name.set(project.name)
                    description.set("ShareFile REST API SDK for Java")
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
            sign(the<PublishingExtension>().publications["mavenJava"])
        }

        tasks.withType<Sign>().configureEach {
            onlyIf { !version.toString().endsWith("-SNAPSHOT") }
        }
    }
}

val coverageProjects =
    listOf(
        project(":sharefile-sdk-core"),
        project(":sharefile-sdk-client"),
        project(":sharefile-spring-boot-starter")
    )

tasks.register("test") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs tests for all code-bearing modules."
    dependsOn(coverageProjects.map { "${it.path}:test" })
}

tasks.named("check") {
    dependsOn(coverageProjects.map { "${it.path}:check" })
    dependsOn("jacocoTestReport")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Generates an aggregate JaCoCo coverage report for the SDK modules."
    dependsOn(coverageProjects.map { it.tasks.named("test") })

    val sourceSets =
        coverageProjects.map { project ->
            project.extensions.getByType<SourceSetContainer>().named("main").get()
        }

    classDirectories.from(sourceSets.map { it.output })
    sourceDirectories.from(sourceSets.flatMap { it.allSource.srcDirs })
    additionalSourceDirs.from(sourceSets.flatMap { it.allSource.srcDirs })
    executionData.from(
        coverageProjects.map { project ->
            project.layout.buildDirectory.file("jacoco/test.exec")
        }
    )

    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/test/html"))
        csv.required.set(false)
    }
}

tasks.register("generateCoverageBadge") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Generates the JaCoCo badge JSON from the aggregate XML report."
    dependsOn("jacocoTestReport")

    val reportFile = layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml")
    val badgeFile = layout.projectDirectory.file(".github/badges/jacoco.json")

    inputs.file(reportFile)
    outputs.file(badgeFile)

    doLast {
        val xmlFile = reportFile.get().asFile
        require(xmlFile.exists()) { "JaCoCo XML report not found: ${xmlFile.absolutePath}" }

        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
        val document = factory.newDocumentBuilder().parse(xmlFile)
        val counters = document.getElementsByTagName("counter")

        var covered = 0L
        var missed = 0L
        for (index in 0 until counters.length) {
            val element = counters.item(index) as Element
            if (element.getAttribute("type") == "INSTRUCTION") {
                covered = element.getAttribute("covered").toLong()
                missed = element.getAttribute("missed").toLong()
                break
            }
        }

        val total = covered + missed
        val percentage =
            if (total == 0L) {
                BigDecimal.ZERO
            } else {
                BigDecimal.valueOf(covered * 100.0 / total).setScale(1, java.math.RoundingMode.HALF_UP)
            }
        val color =
            when {
                percentage >= BigDecimal("80.0") -> "brightgreen"
                percentage >= BigDecimal("70.0") -> "yellowgreen"
                percentage >= BigDecimal("60.0") -> "yellow"
                else -> "red"
            }

        val output = badgeFile.asFile
        output.parentFile.mkdirs()
        output.writeText(
            """
            {
              "schemaVersion": 1,
              "label": "coverage",
              "message": "${percentage.stripTrailingZeros().toPlainString()}%",
              "color": "$color"
            }
            """.trimIndent() + "\n"
        )
    }
}
