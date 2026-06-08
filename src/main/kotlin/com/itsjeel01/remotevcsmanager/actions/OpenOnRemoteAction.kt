package com.itsjeel01.remotevcsmanager.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.VirtualFile
import com.itsjeel01.remotevcsmanager.GitRemoteDetector
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.Icons
import kotlin.io.path.Path

class OpenOnRemoteAction : AnAction(), DumbAware {

    private val provider = GitHubProvider()

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (project == null || file == null) {
            e.presentation.isEnabled = false
            return
        }
        val gitDetector = GitRemoteDetector(project)
        e.presentation.isEnabled = gitDetector.hasRemote() && provider.isConfigured()
        e.presentation.isVisible = true
        e.presentation.text = "Open on GitHub"
        e.presentation.description = "Open this file on GitHub"
        e.presentation.icon = Icons.OPEN_IN_BROWSER_ICON
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val gitDetector = GitRemoteDetector(project)
        val info = gitDetector.detect() ?: return

        val parsed = gitDetector.parseRemoteUrl(info.remoteUrl) ?: return
        val relativePath = file.toNioPath().toString()
            .removePrefix(info.gitRoot.toPath().toString() + java.io.File.separator)
            .replace(java.io.File.separator, "/")
        val branch = info.currentBranch ?: "main"
        val editor = e.getData(CommonDataKeys.EDITOR)
        val lineNumber = editor?.caretModel?.logicalPosition?.line?.plus(1)

        val url = provider.getFileUrl(parsed.owner, parsed.repoName, relativePath, branch, lineNumber)
        BrowserUtil.browse(url)
    }
}
