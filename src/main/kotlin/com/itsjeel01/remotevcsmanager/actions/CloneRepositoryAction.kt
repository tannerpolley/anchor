package com.itsjeel01.remotevcsmanager.actions

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.itsjeel01.remotevcsmanager.models.RemoteRepository
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.Icons
import com.itsjeel01.remotevcsmanager.ui.PluginNotifications
import com.itsjeel01.remotevcsmanager.ui.RepositorySelectionDialog
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Action to clone a remote repository.
 * Shows a dialog listing accessible repositories, then clones the selected one.
 */
class CloneRepositoryAction : AnAction(), DumbAware {

    private val provider = GitHubProvider()

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = provider.isConfigured()
        e.presentation.text = "Clone Remote Repository..."
        e.presentation.description = "Clone a repository from GitHub"
        e.presentation.icon = Icons.CLONE_ICON
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        if (!provider.isConfigured()) {
            PluginNotifications.error(project, "GitHub not configured",
                "Please set up your GitHub token in Settings > Remote VCS Manager")
            return
        }


        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Fetching repositories...", true) {
            private var repos: List<RemoteRepository> = emptyList()
            private var error: String? = null

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                try {
                    repos = runBlocking { provider.getRepositories() }
                } catch (ex: Exception) {
                    error = ex.message
                }
            }

            override fun onSuccess() {
                val err = error
                if (err != null) {
                    PluginNotifications.error(project, "Failed to fetch repositories", err)
                    return
                }

                if (repos.isEmpty()) {
                    PluginNotifications.info(project, "No repositories found",
                        "No GitHub repositories were found for your account.")
                    return
                }

                showRepositorySelectionDialog(project, repos)
            }
        })
    }

    /**
     * Show a dialog to select a repository to clone.
     */
    private fun showRepositorySelectionDialog(project: com.intellij.openapi.project.Project, repos: List<RemoteRepository>) {
        val dialog = RepositorySelectionDialog(project, repos)
        if (dialog.showAndGet()) {
            val selectedRepo = dialog.getSelectedRepository() ?: return
            cloneRepository(project, selectedRepo)
        }
    }

    /**
     * Clone the selected repository using git command line.
     */
    private fun cloneRepository(project: com.intellij.openapi.project.Project, repo: RemoteRepository) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Cloning ${repo.name}...", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true

                try {

                    val baseDir = File(project.basePath ?: System.getProperty("user.home"))
                    val cloneDir = File(baseDir, repo.name)


                    val process = ProcessBuilder(
                        "git", "clone", repo.cloneUrl, cloneDir.absolutePath
                    ).redirectErrorStream(true).start()

                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        indicator.text = line ?: ""
                    }
                    val exitCode = process.waitFor()

                    if (exitCode != 0) {
                        throw Exception("git clone failed with exit code $exitCode")
                    }


                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        ProjectUtil.openOrImport(cloneDir.toPath())
                    }
                } catch (ex: Exception) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        PluginNotifications.error(project, "Clone failed",
                            "Failed to clone ${repo.name}: ${ex.message}")
                    }
                }
            }
        })
    }
}
