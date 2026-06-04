package com.itsjeel01.remotevcsmanager.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.Icons
import com.itsjeel01.remotevcsmanager.ui.PluginNotifications
import java.io.File
import javax.swing.*

/**
 * Action to add a remote to the current project's Git repository.
 * Shows a dialog to enter remote details and adds it as a git remote.
 */
class AddRemoteAction : AnAction(), DumbAware {

    private val provider = GitHubProvider()

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        val gitRoot = findGitRoot(project.basePath)
        e.presentation.isEnabled = gitRoot != null && provider.isConfigured()
        e.presentation.text = "Add Remote from GitHub..."
        e.presentation.description = "Add a GitHub repository as a remote to this project"
        e.presentation.icon = Icons.ADD_REMOTE_ICON
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        if (!provider.isConfigured()) {
            PluginNotifications.error(project, "GitHub not configured",
                "Please set up your GitHub token in Settings > Remote VCS Manager")
            return
        }

        val gitRoot = findGitRoot(project.basePath)
        if (gitRoot == null) {
            PluginNotifications.error(project, "No Git repository",
                "This project does not appear to be a Git repository.")
            return
        }

        // Show dialog for entering remote details
        val dialog = AddRemoteDialog()
        if (!dialog.showAndGet()) return

        val remoteName = dialog.remoteName
        val owner = dialog.owner
        val repoName = dialog.repoName

        if (remoteName.isBlank() || owner.isBlank() || repoName.isBlank()) {
            Messages.showErrorDialog(project, "All fields are required.", "Add Remote")
            return
        }

        try {
            val cloneUrl = provider.getCloneUrl(owner, repoName)
            addGitRemote(gitRoot, remoteName, cloneUrl)

            PluginNotifications.info(project, "Remote added",
                "Remote '$remoteName' added successfully → $cloneUrl")
        } catch (ex: Exception) {
            PluginNotifications.error(project, "Failed to add remote",
                ex.message ?: "Unknown error")
        }
    }

    /**
     * Find the git root directory for a given base path.
     */
    private fun findGitRoot(basePath: String?): File? {
        if (basePath == null) return null
        var current = File(basePath)
        while (current != null) {
            if (File(current, ".git").exists()) {
                return current
            }
            current = current.parentFile
        }
        return null
    }

    /**
     * Execute git command to add a remote.
     */
    private fun addGitRemote(gitRoot: File, name: String, url: String) {
        val process = Runtime.getRuntime().exec(
            arrayOf("git", "remote", "add", name, url),
            null,
            gitRoot
        )
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val error = process.errorStream.bufferedReader().readText()
            throw Exception(error.ifBlank { "Failed to add remote '$name'" })
        }
    }

    /**
     * Dialog for adding a remote repository.
     */
    private class AddRemoteDialog : DialogWrapper(null) {
        private val remoteNameField: JBTextField = JBTextField().apply {
            text = "upstream"
            emptyText.text = "Remote name (e.g., upstream)"
        }
        private val ownerField: JBTextField = JBTextField().apply {
            emptyText.text = "Repository owner (e.g., octocat)"
        }
        private val repoNameField: JBTextField = JBTextField().apply {
            emptyText.text = "Repository name (e.g., hello-world)"
        }

        var remoteName: String = ""
            private set
        var owner: String = ""
            private set
        var repoName: String = ""
            private set

        init {
            title = "Add Remote from GitHub"
            init()
        }

        override fun createCenterPanel(): JComponent {
            val panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Remote Name:", remoteNameField, true)
                .addLabeledComponent("Repository Owner:", ownerField, true)
                .addLabeledComponent("Repository Name:", repoNameField, true)
                .addVerticalGap(5)
                .addComponent(JLabel("<html><small>This will run: git remote add &lt;name&gt; https://github.com/&lt;owner&gt;/&lt;repo&gt;.git</small></html>"))
                .panel

            panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            return panel
        }

        override fun doOKAction() {
            remoteName = remoteNameField.text.trim()
            owner = ownerField.text.trim()
            repoName = repoNameField.text.trim()

            if (remoteName.isBlank() || owner.isBlank() || repoName.isBlank()) {
                Messages.showErrorDialog("All fields are required.", "Invalid Input")
                return
            }

            super.doOKAction()
        }
    }
}
