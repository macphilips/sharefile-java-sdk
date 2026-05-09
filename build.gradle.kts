import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

plugins {
    base
    id("com.diffplug.spotless") version "8.4.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

abstract class InstallGitHookTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceHook: RegularFileProperty

    @get:OutputFile
    abstract val installedHook: RegularFileProperty

    @TaskAction
    fun install() {
        val source = sourceHook.get().asFile
        val target = installedHook.get().asFile
        val gitDirectory = target.parentFile.parentFile

        require(gitDirectory.exists() && gitDirectory.name == ".git") {
            "Cannot install Git hooks because this checkout does not contain a .git directory."
        }

        target.parentFile.mkdirs()
        source.copyTo(target, overwrite = true)
        target.setExecutable(true, false)
    }
}

repositories {
    mavenCentral()
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

val javaStyleProjects = subprojects.filter { it.name != "sharefile-sdk-bom" }
spotless {
    java {
        target("sharefile-sdk-*/src/*/java/**/*.java")
        targetExclude("**/build/**", "**/generated/**")
        googleJavaFormat("1.35.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("prettier") {
        target(
            "*.md",
            "docs/**/*.md",
            "**/*.json",
            "**/*.yml",
            "**/*.yaml",
            "**/*.html",
            "**/*.css",
            "**/*.js",
            "**/*.jsx",
            "**/*.ts",
            "**/*.tsx"
        )
        targetExclude(
            "**/.git/**",
            "**/.gradle/**",
            "**/.gradle-local/**",
            "**/.idea/**",
            "**/build/**",
            "**/dist/**",
            "**/node_modules/**",
            "**/out/**",
            "**/target/**"
        )
        prettier(mapOf("prettier" to "3.5.3"))
            .npmExecutable(rootProject.file("scripts/npmw").absolutePath)
            .nodeExecutable(rootProject.file("scripts/nodew").absolutePath)
            .configFile(rootProject.file(".prettierrc.json").absolutePath)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.named("check") {
    dependsOn("spotlessCheck")
    dependsOn(javaStyleProjects.map { "${it.path}:check" })
}

tasks.register("checkstyleMain") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs Checkstyle on main sources for all Java modules."
    dependsOn(javaStyleProjects.map { "${it.path}:checkstyleMain" })
}

tasks.register("checkstyleTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs Checkstyle on test sources for all Java modules."
    dependsOn(javaStyleProjects.map { "${it.path}:checkstyleTest" })
}

tasks.register<InstallGitHookTask>("installGitHooks") {
    group = "build setup"
    description = "Installs the repository-local Git hooks into .git/hooks."
    sourceHook.set(layout.projectDirectory.file("scripts/git-hooks/pre-commit"))
    installedHook.set(layout.projectDirectory.file(".git/hooks/pre-commit"))
}

subprojects {
    // BOM uses java-platform with its own publishing config — skip here
    if (name == "sharefile-sdk-bom") return@subprojects

    apply(plugin = "java-library")
    apply(plugin = "checkstyle")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

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

    configure<CheckstyleExtension> {
        toolVersion = "10.21.4"
        configDirectory.set(rootProject.layout.projectDirectory.dir("config/checkstyle"))
        isIgnoreFailures = false
        maxWarnings = 0
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
    }

    tasks.withType<Checkstyle>().configureEach {
        exclude("**/generated/**")
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
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
