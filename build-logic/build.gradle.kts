plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.4.0")
    implementation("com.github.node-gradle:gradle-node-plugin:7.1.0")
}
