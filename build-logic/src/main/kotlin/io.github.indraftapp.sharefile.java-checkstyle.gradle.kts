import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.plugins.quality.Checkstyle

plugins {
    checkstyle
}

configure<CheckstyleExtension> {
    toolVersion = "10.21.4"
    configDirectory.set(rootProject.layout.projectDirectory.dir("config/checkstyle"))
    isIgnoreFailures = false
    maxWarnings = 0
}

tasks.withType<Checkstyle>().configureEach {
    exclude("**/generated/**")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
