package com.itsjeel01.remotevcsmanager.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.detail.SwingIssueDetailRenderer
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import java.util.concurrent.atomic.AtomicLong
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import kotlinx.coroutines.runBlocking

internal class RepoIssuesTreePanel(
    private val project: Project,
    private val provider: GitHubProvider,
    private val targets: List<RepoIssueTarget>,
    private val detailRenderer: SwingIssueDetailRenderer
) {

    private val rootNode = DefaultMutableTreeNode("GitHub Issues")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel)
    private val status = JBLabel()
    private val openIssueButton = JButton("Open Issue")
    private val openRepoButton = JButton("Open Repo")
    private val detailRequests = AtomicLong()
    private val treeRequests = AtomicLong()
    private var selectedTarget: RepoIssueTarget = targets.first()
    private var selectedIssue: Issue? = null

    val component: JComponent = createComponent()

    init {
        configureTree()
        reloadIssues()
    }

    private fun createComponent(): JComponent {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.add(createHeader(), BorderLayout.NORTH)
        panel.add(createSplitPane(), BorderLayout.CENTER)
        return panel
    }

    private fun createHeader(): JComponent {
        val header = JBPanel<JBPanel<*>>(BorderLayout())
        header.border = JBUI.Borders.emptyBottom(8)

        val title = JBLabel("GitHub Issues").apply {
            font = font.deriveFont(Font.BOLD)
        }
        header.add(title, BorderLayout.WEST)

        val actions = JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(6), 0))
        val refresh = JButton("Refresh").apply {
            addActionListener { reloadIssues() }
        }
        openIssueButton.apply {
            isEnabled = false
            addActionListener {
                selectedIssue?.let { BrowserUtil.browse(it.url) }
            }
        }
        openRepoButton.apply {
            addActionListener { BrowserUtil.browse(selectedTarget.issuesUrl) }
        }

        actions.add(status)
        actions.add(refresh)
        actions.add(openIssueButton)
        actions.add(openRepoButton)
        header.add(actions, BorderLayout.EAST)
        return header
    }

    private fun createSplitPane(): JComponent =
        JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            JBScrollPane(tree),
            detailRenderer.component
        ).apply {
            border = JBUI.Borders.empty()
            resizeWeight = 0.34
            dividerLocation = JBUI.scale(390)
        }

    private fun configureTree(): Unit {
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.cellRenderer = RepoIssuesTreeRenderer()
        tree.emptyText.text = "No GitHub repositories"
        tree.selectionModel.selectionMode = javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.addTreeSelectionListener {
            handleSelection(tree.selectionPath)
        }
    }

    private fun reloadIssues(): Unit {
        val requestId = treeRequests.incrementAndGet()
        detailRequests.incrementAndGet()
        selectedIssue = null
        syncButtons()
        status.text = "Loading ${targets.size} repos..."
        status.foreground = UIUtil.getContextHelpForeground()
        detailRenderer.showPlaceholder("Select an issue under a repository to read its body and comments.")
        showLoadingNodes()

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = targets.map { target ->
                target to runCatching {
                    runBlocking {
                        provider.getIssues(target.owner, target.repoName, "open", null, null)
                    }
                }
            }

            SwingUtilities.invokeLater {
                if (project.isDisposed || treeRequests.get() != requestId) return@invokeLater
                showIssueNodes(result)
            }
        }
    }

    private fun showLoadingNodes(): Unit {
        rootNode.removeAllChildren()
        targets.forEach { target ->
            val repoNode = DefaultMutableTreeNode(RepoIssueTreeItem.Repository(target, null))
            repoNode.add(DefaultMutableTreeNode(RepoIssueTreeItem.Message("Loading issues...")))
            rootNode.add(repoNode)
        }
        treeModel.reload()
        expandRepositoryNodes()
    }

    private fun showIssueNodes(results: List<Pair<RepoIssueTarget, Result<List<Issue>>>>): Unit {
        rootNode.removeAllChildren()
        var total = 0
        var failures = 0

        results.forEach { (target, result) ->
            result.onSuccess { issues ->
                total += issues.size
                val repoNode = DefaultMutableTreeNode(RepoIssueTreeItem.Repository(target, issues.size))
                if (issues.isEmpty()) {
                    repoNode.add(DefaultMutableTreeNode(RepoIssueTreeItem.Message("No open issues")))
                } else {
                    issues.forEach { issue ->
                        repoNode.add(DefaultMutableTreeNode(RepoIssueTreeItem.IssueNode(target, issue)))
                    }
                }
                rootNode.add(repoNode)
            }.onFailure { error ->
                failures += 1
                val repoNode = DefaultMutableTreeNode(RepoIssueTreeItem.Repository(target, null))
                repoNode.add(
                    DefaultMutableTreeNode(
                        RepoIssueTreeItem.Message(error.message ?: "GitHub API request failed")
                    )
                )
                rootNode.add(repoNode)
            }
        }

        treeModel.reload()
        expandRepositoryNodes()
        status.text = statusText(total, failures)
        status.foreground = if (failures == 0) UIUtil.getContextHelpForeground() else UIUtil.getErrorForeground()
    }

    private fun handleSelection(path: TreePath?): Unit {
        val item = (path?.lastPathComponent as? DefaultMutableTreeNode)?.userObject
        when (item) {
            is RepoIssueTreeItem.Repository -> {
                selectedTarget = item.target
                selectedIssue = null
                detailRequests.incrementAndGet()
                detailRenderer.showPlaceholder("Select an issue under ${item.target.displayName}.")
            }
            is RepoIssueTreeItem.IssueNode -> {
                selectedTarget = item.target
                selectedIssue = item.issue
                loadIssueDetail(item.target, item.issue)
            }
            else -> selectedIssue = null
        }
        syncButtons()
    }

    private fun loadIssueDetail(target: RepoIssueTarget, issue: Issue): Unit {
        val requestId = detailRequests.incrementAndGet()
        detailRenderer.showLoading(issue)
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching {
                runBlocking {
                    val context = "${target.owner}/${target.repoName}"
                    val comments = provider.getIssueComments(target.owner, target.repoName, issue.number)
                    val body = renderMarkdown(issue.body.orEmpty(), context)
                    val renderedComments = comments.map { renderMarkdown(it.body, context) }
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

    private suspend fun renderMarkdown(markdown: String, context: String): String {
        if (markdown.isBlank()) return "<p><em>No description provided.</em></p>"
        val rendered = provider.renderMarkdown(markdown, context)
        return if (rendered == markdown) "<pre>${markdown.escapeHtml()}</pre>" else rendered
    }

    private fun expandRepositoryNodes(): Unit {
        var row = 0
        while (row < tree.rowCount) {
            tree.expandRow(row)
            row += 1
        }
    }

    private fun statusText(total: Int, failures: Int): String =
        if (failures == 0) {
            "$total open"
        } else {
            "$total open, $failures failed"
        }

    private fun syncButtons(): Unit {
        openIssueButton.isEnabled = selectedIssue != null
        openRepoButton.isEnabled = selectedTarget.issuesUrl.isNotBlank()
    }

    private fun String.escapeHtml(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    private data class IssueDetail(
        val comments: List<IssueComment>,
        val body: String,
        val renderedComments: List<String>
    )
}

internal sealed interface RepoIssueTreeItem {
    data class Repository(
        val target: RepoIssueTarget,
        val openIssueCount: Int?
    ) : RepoIssueTreeItem

    data class IssueNode(
        val target: RepoIssueTarget,
        val issue: Issue
    ) : RepoIssueTreeItem

    data class Message(
        val text: String
    ) : RepoIssueTreeItem
}
