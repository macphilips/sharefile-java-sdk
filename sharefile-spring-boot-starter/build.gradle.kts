plugins {
    id("org.springframework.boot") version "3.3.5" apply false
}

val jacksonVersion = "2.18.6"

dependencies {
    api(project(":sharefile-sdk-client"))
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    implementation(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))
    compileOnly(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    compileOnly(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))
    annotationProcessor(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    annotationProcessor(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))
    testImplementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    testImplementation(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-core")

    compileOnly("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    compileOnly("io.micrometer:micrometer-tracing")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
