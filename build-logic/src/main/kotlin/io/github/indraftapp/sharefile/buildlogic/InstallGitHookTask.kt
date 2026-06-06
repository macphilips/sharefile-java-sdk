package io.github.indraftapp.sharefile.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

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
