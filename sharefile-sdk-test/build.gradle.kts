dependencies {
    api(project(":sharefile-sdk-client"))

    api(platform("org.junit:junit-bom:5.11.3"))
    api("org.junit.jupiter:junit-jupiter")
    api("org.assertj:assertj-core:3.26.3")
    api("org.wiremock:wiremock-standalone:3.9.2")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
