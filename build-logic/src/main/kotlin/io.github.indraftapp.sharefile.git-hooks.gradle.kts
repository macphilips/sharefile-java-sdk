import io.github.indraftapp.sharefile.buildlogic.InstallGitHookTask
import org.gradle.language.base.plugins.LifecycleBasePlugin

val eligibleProjectNames = setOf(
    "sharefile-sdk-core",
    "sharefile-sdk-client",
    "sharefile-spring-boot-starter",
    "sharefile-sdk-test"
)

val checkstyleMainAggregate =
    tasks.register("checkstyleMain") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Runs Checkstyle on main sources for all Java modules."
    }

val checkstyleTestAggregate =
    tasks.register("checkstyleTest") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Runs Checkstyle on test sources for all Java modules."
    }

tasks.register<InstallGitHookTask>("installGitHooks") {
    group = "build setup"
    description = "Installs the repository-local Git hooks into .git/hooks."
    sourceHook.set(layout.projectDirectory.file("scripts/git-hooks/pre-commit"))
    installedHook.set(layout.projectDirectory.file(".git/hooks/pre-commit"))
}

subprojects {
    if (name !in eligibleProjectNames) {
        return@subprojects
    }

    val projectPath = path

    pluginManager.withPlugin("checkstyle") {
        checkstyleMainAggregate.configure {
            dependsOn("$projectPath:checkstyleMain")
        }
        checkstyleTestAggregate.configure {
            dependsOn("$projectPath:checkstyleTest")
        }
    }

    pluginManager.withPlugin("java-base") {
        rootProject.tasks.named("check") {
            dependsOn("$projectPath:check")
        }
    }
}
