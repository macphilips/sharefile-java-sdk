import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.gradle.node.NodeExtension

plugins {
    base
    id("com.diffplug.spotless")
    id("com.github.node-gradle.node")
}

configure<NodeExtension> {
    download.set(true)
    version.set("20.19.0")
}

val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
val nodeExtension = the<NodeExtension>()
val nodeExecutablePath =
    nodeExtension.resolvedNodeDir.map { nodeDir ->
        val relativePath = if (isWindows) "node.exe" else "bin/node"
        nodeDir.file(relativePath).asFile.absolutePath
    }
val npmExecutablePath =
    nodeExtension.resolvedNodeDir.map { nodeDir ->
        val relativePath = if (isWindows) "npm.cmd" else "bin/npm"
        nodeDir.file(relativePath).asFile.absolutePath
    }

configure<SpotlessExtension> {
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
            "**/.gradle-local*/**",
            "**/.idea/**",
            "**/build/**",
            "**/dist/**",
            "**/node_modules/**",
            "**/out/**",
            "**/target/**"
        )
        prettier(mapOf("prettier" to "3.5.3"))
            .npmExecutable(npmExecutablePath.get())
            .nodeExecutable(nodeExecutablePath.get())
            .configFile(rootProject.file(".prettierrc.json").absolutePath)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.matching { it.name.startsWith("spotless") }.configureEach {
    dependsOn("nodeSetup")
}

tasks.named("check") {
    dependsOn("spotlessCheck")
}
