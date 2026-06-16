package com.itsjeel01.remotevcsmanager.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.itsjeel01.remotevcsmanager.GitRemoteDetector
import com.itsjeel01.remotevcsmanager.GitRemoteDetector.GitRemoteInfo
import com.itsjeel01.remotevcsmanager.GitRootDiscovery
import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.providers.github.GitHubAuth
import com.itsjeel01.remotevcsmanager.providers.github.JetBrainsGithubTokenProvider
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.detail.SwingIssueDetailRenderer
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicLong
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTextArea
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import kotlinx.coroutines.runBlocking

object RemoteVcsIssuesPanel {

    fun create(project: Project): JComponent {
        val targets = resolveTargets(project)
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.border = JBUI.Borders.empty(8)

        if (targets.isEmpty()) {
            panel.add(createMessagePanel("Anchor could not detect a GitHub remote for this project."), BorderLayout.CENTER)
            return panel
        }

        val selected = SelectedTarget(targets.first())
        val provider = createProvider(project)
        val detailRenderer = SwingIssueDetailRenderer()
        val detailRequests = AtomicLong()
        val issuesModel = DefaultListModel<Issue>()
        val status = JBLabel()
        val issuesList = createIssuesList(issuesModel)
        Disposer.register(project, detailRenderer)

        panel.add(
            createHeader(project, provider, selected, status, issuesModel, issuesList, detailRenderer, detailRequests),
            BorderLayout.NORTH
        )
        panel.add(
            createContent(project, provider, targets, selected, issuesModel, status, issuesList, detailRenderer, detailRequests),
            BorderLayout.CENTER
        )
        loadIssues(project, provider, selected.target, issuesModel, status, detailRenderer, detailRequests)
        return panel
    }

    private fun createHeader(
        project: Project,
        provider: GitHubProvider,
        selected: SelectedTarget,
        status: JBLabel,
        issuesModel: DefaultListModel<Issue>,
        issuesList: JBList<Issue>,
        detailRenderer: SwingIssueDetailRenderer,
        detailRequests: AtomicLong
    ): JComponent {
        val header = JBPanel<JBPanel<*>>(BorderLayout())
        header.border = JBUI.Borders.emptyBottom(8)

        val title = JBLabel("GitHub Issues").apply {
            font = font.deriveFont(Font.BOLD)
        }
        header.add(title, BorderLayout.WEST)

        val actions = JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(6), 0))
        val openIssue = JButton("Open Issue").apply {
            isEnabled = false
            addActionListener {
                issuesList.selectedValue?.let { BrowserUtil.browse(it.url) }
            }
        }
        issuesList.addListSelectionListener {
            openIssue.isEnabled = issuesList.selectedValue != null
        }

        val refresh = JButton("Refresh").apply {
            addActionListener {
                loadIssues(project, provider, selected.target, issuesModel, status, detailRenderer, detailRequests)
            }
        }
        val openRepo = JButton("Open in Browser").apply {
            addActionListener { BrowserUtil.browse(selected.target.issuesUrl) }
        }

        actions.add(status)
        actions.add(refresh)
        actions.add(openIssue)
        actions.add(openRepo)
        header.add(actions, BorderLayout.EAST)
        return header
    }

    private fun createContent(
        project: Project,
        provider: GitHubProvider,
        targets: List<RepoTarget>,
        selected: SelectedTarget,
        issuesModel: DefaultListModel<Issue>,
        status: JBLabel,
        issuesList: JBList<Issue>,
        detailRenderer: SwingIssueDetailRenderer,
        detailRequests: AtomicLong
    ): JComponent {
        issuesList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                val issue = issuesList.selectedValue
                if (issue == null) {
                    detailRequests.incrementAndGet()
                    detailRenderer.showPlaceholder("Select an issue to read its body and comments.")
                } else {
                    loadIssueDetail(project, provider, selected.target, issue, detailRenderer, detailRequests)
                }
            }
        }

        val issueDetail = JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            JBScrollPane(issuesList),
            detailRenderer.component
        ).apply {
            border = JBUI.Borders.empty()
            resizeWeight = 0.38
            dividerLocation = JBUI.scale(430)
        }

        if (targets.size == 1) {
            return issueDetail
        }

        val repoList = createRepoList(targets)
        repoList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                val target = repoList.selectedValue ?: return@addListSelectionListener
                selected.target = target
                loadIssues(project, provider, target, issuesModel, status, detailRenderer, detailRequests)
            }
        }

        return JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            JBScrollPane(repoList),
            issueDetail
        ).apply {
            border = JBUI.Borders.empty()
            resizeWeight = 0.0
            dividerLocation = JBUI.scale(220)
        }
    }

    private fun createRepoList(targets: List<RepoTarget>): JBList<RepoTarget> =
        JBList(targets).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            selectedIndex = 0
            cellRenderer = RepoTargetRenderer()
            visibleRowCount = 12
        }

    private fun createIssuesList(model: DefaultListModel<Issue>): JBList<Issue> =
        JBList(model).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            cellRenderer = IssueRenderer()
            emptyText.text = "No open issues"
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        selectedValue?.let { BrowserUtil.browse(it.url) }
                    }
                }
            })
        }

    private fun loadIssues(
        project: Project?,
        provider: GitHubProvider,
        target: RepoTarget,
        model: DefaultListModel<Issue>,
        status: JBLabel,
        detailRenderer: SwingIssueDetailRenderer,
        detailRequests: AtomicLong
    ) {
        detailRequests.incrementAndGet()
        model.clear()
        detailRenderer.showPlaceholder("Select an issue to read its body and comments.")
        status.text = "Loading ${target.displayName}..."
        status.foreground = UIUtil.getContextHelpForeground()

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching {
                runBlocking {
                    provider.getIssues(target.owner, target.repoName, "open", null, null)
                }
            }

            SwingUtilities.invokeLater {
                if (project?.isDisposed == true) return@invokeLater
                model.clear()
                result.onSuccess { issues ->
                    issues.forEach(model::addElement)
                    status.text = "${issues.size} open"
                    status.foreground = UIUtil.getContextHelpForeground()
                }.onFailure { error ->
                    status.text = error.message ?: "GitHub API request failed"
                    status.foreground = UIUtil.getErrorForeground()
                }
            }
        }
    }

    private fun loadIssueDetail(
        project: Project,
        provider: GitHubProvider,
        target: RepoTarget,
        issue: Issue,
        detailRenderer: SwingIssueDetailRenderer,
        detailRequests: AtomicLong
    ) {
        val requestId = detailRequests.incrementAndGet()
        detailRenderer.showLoading(issue)
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching {
                runBlocking {
                    val context = "${target.owner}/${target.repoName}"
                    val comments = provider.getIssueComments(target.owner, target.repoName, issue.number)
                    val body = renderMarkdown(provider, issue.body.orEmpty(), context)
                    val renderedComments = comments.map { renderMarkdown(provider, it.body, context) }
                    IssueDetail(comments, body, renderedComments)
                }
            }

            SwingUtilities.invokeLater {
                if (project.isDisposed || detailRequests.get() != requestId) return@invokeLater
                result.onSuccess { detail ->
                    detailRenderer.showIssue(issue, detail.comments, detail.body, detail.renderedComments)
                }.onFailure { error ->
                    detailRenderer.showError(issue, error.message ?: "GitHub API request failed")
                }
            }
        }
    }

    private suspend fun renderMarkdown(provider: GitHubProvider, markdown: String, context: String): String {
        if (markdown.isBlank()) return "<p><em>No description provided.</em></p>"
        val rendered = provider.renderMarkdown(markdown, context)
        return if (rendered == markdown) "<pre>${markdown.escapeHtml()}</pre>" else rendered
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

    private fun resolveTargets(project: Project): List<RepoTarget> {
        val detector = GitRemoteDetector(project)
        return GitRootDiscovery.roots(project).mapNotNull { root ->
            detector.detect(root)?.let(::createTarget)
        }.distinctBy { it.issuesUrl }
            .sortedBy { it.displayName.lowercase() }
    }

    private fun createTarget(remote: GitRemoteInfo): RepoTarget? {
        if (remote.provider != "github") return null
        val host = parseRemoteHost(remote.remoteUrl) ?: "github.com"
        return RepoTarget(
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

    private class RepoTargetRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is RepoTarget) {
                text = value.displayName
                toolTipText = value.rootPath
            }
            return component
        }
    }

    private class IssueRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is Issue) {
                text = issueText(value)
                toolTipText = value.url
            }
            return component
        }

        private fun issueText(issue: Issue): String {
            val labels = issue.labels.take(5).joinToString(" ") { escapeHtml(it.name) }
            val labelText = if (labels.isBlank()) "" else "<br><span>$labels</span>"
            val meta = "#${issue.number} by ${escapeHtml(issue.author)} · ${TimeFormat.relative(issue.updatedAt)}"
            return """
                <html>
                  <b>${escapeHtml(issue.title)}</b><br>
                  <span>${escapeHtml(meta)}</span>$labelText
                </html>
            """.trimIndent()
        }

        private fun escapeHtml(value: String): String =
            value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
    }

    private data class SelectedTarget(var target: RepoTarget)

    private data class IssueDetail(
        val comments: List<IssueComment>,
        val body: String,
        val renderedComments: List<String>
    )

    private data class RepoTarget(
        val displayName: String,
        val owner: String,
        val repoName: String,
        val rootPath: String,
        val issuesUrl: String
    )

    private fun String.escapeHtml(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
}
