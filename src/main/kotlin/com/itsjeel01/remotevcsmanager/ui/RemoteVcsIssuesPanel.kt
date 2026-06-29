package com.itsjeel01.remotevcsmanager.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.itsjeel01.remotevcsmanager.GitRemoteDetector
import com.itsjeel01.remotevcsmanager.GitRemoteDetector.GitRemoteInfo
import com.itsjeel01.remotevcsmanager.GitRootDiscovery
import com.itsjeel01.remotevcsmanager.providers.github.GitHubAuth
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.providers.github.JetBrainsGithubTokenProvider
import com.itsjeel01.remotevcsmanager.ui.editor.IssueEditorPreviewOpener
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JTextArea

object RemoteVcsIssuesPanel {

    fun create(project: Project): JComponent {
        val accountLogins = JetBrainsGithubTokenProvider.getAccountLogins(project)
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.border = JBUI.Borders.empty(8)

        if (accountLogins.isEmpty()) {
            panel.add(createMessagePanel(emptyTargetsMessage(accountLogins)), BorderLayout.CENTER)
            return panel
        }

        val targets = resolveTargets(project)
        if (targets.isEmpty()) {
            panel.add(createMessagePanel(emptyTargetsMessage(accountLogins)), BorderLayout.CENTER)
            return panel
        }

        val provider = createProvider(project)
        val inclusionSettings = RepoIssueInclusionSettings.getInstance(project)

        val treePanel = RepoIssuesTreePanel(
            project = project,
            provider = provider,
            allTargets = targets,
            initialInclusionState = inclusionSettings.inclusionState(),
            onInclusionChanged = inclusionSettings::setInclusionState,
            accountLogins = accountLogins,
            previewOpener = IssueEditorPreviewOpener(project, provider)
        )
        panel.add(treePanel.component, BorderLayout.CENTER)
        return panel
    }

    private fun resolveTargets(project: Project): List<RepoIssueTarget> {
        val detector = GitRemoteDetector(project)
        return GitRootDiscovery.roots(project).mapNotNull { root ->
            detector.detect(root)?.let(::createTarget)
        }.distinctBy { it.issuesUrl }
            .sortedBy { it.displayName.lowercase() }
    }

    private fun createTarget(remote: GitRemoteInfo): RepoIssueTarget? {
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

    private fun emptyTargetsMessage(accountLogins: Set<String>): String =
        if (accountLogins.isEmpty()) {
            "Sign in to GitHub in IntelliJ IDEA to show issues for remotes owned by your account."
        } else {
            "Anchor could not detect a GitHub remote for this project."
        }

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
