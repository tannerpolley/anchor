package com.itsjeel01.remotevcsmanager.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.Icons
import java.awt.datatransfer.StringSelection
import java.awt.Desktop
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URI

/**
 * Action to open the current file on the remote (e.g., GitHub).
 * Works with the currently active file editor.
 */
class OpenOnRemoteAction : AnAction(), DumbAware {

    private val provider = GitHubProvider()

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            e.presentation.isVisible = false
            return
        }

        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (file == null) {
            e.presentation.isEnabled = false
            e.presentation.text = "Open on GitHub"
            return
        }

        val gitRoot = findGitRoot(file)
        if (gitRoot == null || !provider.isConfigured()) {
            e.presentation.isEnabled = false
            return
        }

        e.presentation.isEnabled = true
        e.presentation.isVisible = true
        e.presentation.text = "Open on GitHub"
        e.presentation.description = "Open this file on GitHub"
        e.presentation.icon = Icons.OPEN_IN_BROWSER_ICON
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val gitRoot = findGitRoot(file) ?: return

        try {
            val remoteUrl = getRemoteUrl(gitRoot)
            if (remoteUrl == null) {
                showError(e, "No remote 'origin' configured for this repository")
                return
            }

            val (owner, repo) = parseGithubUrl(remoteUrl) ?: run {
                showError(e, "Remote URL is not a GitHub URL: $remoteUrl")
                return
            }

            // Get relative file path
            val relativePath = file.toNioPath().toString()
                .removePrefix(gitRoot.toPath().toString() + File.separator)
                .replace(File.separator, "/")

            // Get current branch name
            val branch = getCurrentBranch(gitRoot) ?: "main"

            // Get line number from editor
            val editor = e.getData(CommonDataKeys.EDITOR)
            val lineNumber = editor?.let { editor.caretModel.logicalPosition.line + 1 }

            val url = provider.getFileUrl(owner, repo, relativePath, branch, lineNumber)

            // Open in browser
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                // Fallback: copy to clipboard
                CopyPasteManager.getInstance().setContents(StringSelection(url))
                showInfo(e, "URL copied to clipboard:\n$url")
            }
        } catch (ex: Exception) {
            showError(e, "Error: ${ex.message}")
        }
    }

    /**
     * Find the git root directory containing the given file.
     */
    private fun findGitRoot(file: VirtualFile): File? {
        var current = file.toNioPath().toFile()
        while (current != null) {
            if (File(current, ".git").exists()) {
                return current
            }
            current = current.parentFile
        }
        return null
    }

    /**
     * Execute git command and return stdout.
     */
    private fun gitCommand(gitRoot: File, vararg args: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf("git", *args),
                null,
                gitRoot
            )
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            process.waitFor()
            result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the remote 'origin' URL from a git repository.
     */
    private fun getRemoteUrl(gitRoot: File): String? {
        return gitCommand(gitRoot, "remote", "get-url", "origin")
    }

    /**
     * Get the current branch name.
     */
    private fun getCurrentBranch(gitRoot: File): String? {
        return gitCommand(gitRoot, "rev-parse", "--abbrev-ref", "HEAD")
    }

    /**
     * Parse owner and repo from a GitHub URL.
     * Supports: https://github.com/owner/repo.git and git@github.com:owner/repo.git
     */
    private fun parseGithubUrl(url: String): Pair<String, String>? {
        val httpsRegex = Regex("https?://github\\.com/([^/]+)/([^/.]+)(?:\\.git)?")
        val sshRegex = Regex("git@github\\.com:([^/]+)/([^/.]+)(?:\\.git)?")

        httpsRegex.find(url)?.let {
            return it.groupValues[1] to it.groupValues[2]
        }
        sshRegex.find(url)?.let {
            return it.groupValues[1] to it.groupValues[2]
        }

        return null
    }

    private fun showError(e: AnActionEvent, message: String) {
        com.intellij.openapi.ui.Messages.showErrorDialog(e.project, message, "Open on GitHub")
    }

    private fun showInfo(e: AnActionEvent, message: String) {
        com.intellij.openapi.ui.Messages.showInfoMessage(e.project, message, "Open on GitHub")
    }
}
