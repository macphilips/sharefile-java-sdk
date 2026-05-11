plugins {
    id("org.springframework.boot") version "3.3.5" apply false
}

dependencies {
    api(project(":sharefile-sdk-client"))
    api(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))

    api(platform("org.junit:junit-bom:5.11.3"))
    api("org.junit.jupiter:junit-jupiter")
    api("org.assertj:assertj-core:3.26.3")
    api("org.wiremock:wiremock-standalone:3.9.2")
    api("org.springframework:spring-context")
    api("org.springframework:spring-test")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
