package com.queukat.advsqlinjection.settings

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Paths

object AdvancedSQLInjectionHelp {
    fun openReadme(project: Project?): Boolean {
        val basePath = project?.basePath ?: return false
        val readmePath = Paths.get(basePath, "README.md")
        val virtualFile = LocalFileSystem.getInstance()
            .refreshAndFindFileByPath(readmePath.toString().replace('\\', '/'))
            ?: return false

        FileEditorManager.getInstance(project).openFile(virtualFile, true)
        return true
    }
}
