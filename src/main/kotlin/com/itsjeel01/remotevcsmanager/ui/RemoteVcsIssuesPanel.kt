package com.itsjeel01.remotevcsmanager.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.itsjeel01.remotevcsmanager.GitRemoteDetector
import com.itsjeel01.remotevcsmanager.GitRemoteDetector.GitRemoteInfo
import com.itsjeel01.remotevcsmanager.providers.github.GitHubAuth
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.providers.github.JetBrainsGithubTokenProvider
import com.itsjeel01.remotevcsmanager.ui.editor.IssueEditorPreviewOpener
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JTextArea

object RemoteVcsIssuesPanel {

    fun create(project: Project): JComponent {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.border = JBUI.Borders.empty(8)

        val target = resolveTarget(project)
        if (target == null) {
            panel.add(createMessagePanel("Anchor could not detect a GitHub remote for this project."), BorderLayout.CENTER)
            return panel
        }

        val provider = createProvider(project)
        val treePanel = RepoIssuesTreePanel(
            project = project,
            provider = provider,
            target = target,
            previewOpener = IssueEditorPreviewOpener(project, provider)
        )
        panel.add(treePanel.component, BorderLayout.CENTER)
        return panel
    }

    private fun resolveTarget(project: Project): RepoIssueTarget? =
        GitRemoteDetector(project).detect()?.let(::createTarget)

    internal fun createTarget(remote: GitRemoteInfo): RepoIssueTarget? {
        if (remote.provider != "github") return null
        val host = parseRemoteHost(remote.remoteUrl) ?: "github.com"
        return RepoIssueTarget(
            displayName = remote.repoName,
            owner = remote.owner,
            repoName = remote.repoName,
            rootPath = remote.gitRoot.absolutePath,
            issuesUrl = "https://$host/${remote.owner}/${remote.repoName}/issues"
        )
    }

    private fun parseRemoteHost(remoteUrl: String): String? {
        Regex("""https?://([^/]+)/""").find(remoteUrl)?.let {
            return it.groupValues[1]
        }
        Regex("""git@([^:]+):""").find(remoteUrl)?.let {
            return it.groupValues[1]
        }
        Regex("""git://([^/]+)/""").find(remoteUrl)?.let {
            return it.groupValues[1]
        }
        return null
    }

    private fun createProvider(project: Project): GitHubProvider =
        GitHubProvider(GitHubAuth { JetBrainsGithubTokenProvider.getToken(project) })

    private fun createMessagePanel(message: String): JComponent {
        val area = JTextArea(message)
        area.isEditable = false
        area.isOpaque = false
        area.lineWrap = true
        area.wrapStyleWord = true
        area.border = JBUI.Borders.empty(12)
        return area
    }
}
