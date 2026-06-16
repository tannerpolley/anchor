package com.itsjeel01.remotevcsmanager.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
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
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
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
        val issuesModel = DefaultListModel<Issue>()
        val status = JBLabel()
        val issuesList = createIssuesList(issuesModel)

        panel.add(createHeader(project, selected, status, issuesModel, issuesList), BorderLayout.NORTH)
        panel.add(createContent(project, targets, selected, issuesModel, status, issuesList), BorderLayout.CENTER)
        loadIssues(project, selected.target, issuesModel, status)
        return panel
    }

    private fun createHeader(
        project: Project,
        selected: SelectedTarget,
        status: JBLabel,
        issuesModel: DefaultListModel<Issue>,
        issuesList: JBList<Issue>
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
                loadIssues(project, selected.target, issuesModel, status)
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
        targets: List<RepoTarget>,
        selected: SelectedTarget,
        issuesModel: DefaultListModel<Issue>,
        status: JBLabel,
        issuesList: JBList<Issue>
    ): JComponent {
        if (targets.size == 1) {
            return JBScrollPane(issuesList)
        }

        val repoList = createRepoList(targets)
        repoList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                val target = repoList.selectedValue ?: return@addListSelectionListener
                selected.target = target
                loadIssues(project, target, issuesModel, status)
            }
        }

        return JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            JBScrollPane(repoList),
            JBScrollPane(issuesList)
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
        target: RepoTarget,
        model: DefaultListModel<Issue>,
        status: JBLabel
    ) {
        model.clear()
        status.text = "Loading ${target.displayName}..."
        status.foreground = UIUtil.getContextHelpForeground()

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching {
                runBlocking {
                    GitHubProvider().getIssues(target.owner, target.repoName, "open", null, null)
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

    private data class RepoTarget(
        val displayName: String,
        val owner: String,
        val repoName: String,
        val rootPath: String,
        val issuesUrl: String
    )
}
