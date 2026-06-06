pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "sharefile-java-sdk"

include(
    "sharefile-sdk-core",
    "sharefile-sdk-client",
    "sharefile-spring-boot-starter",
    "sharefile-sdk-bom",
    "sharefile-sdk-test"
)
