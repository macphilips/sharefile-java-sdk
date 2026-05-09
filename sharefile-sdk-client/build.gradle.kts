dependencies {
    api(project(":sharefile-sdk-core"))
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.12")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
