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
import com.itsjeel01.remotevcsmanager.models.IssueMilestone
import com.itsjeel01.remotevcsmanager.models.IssueRelationship
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.editor.IssueEditorPreviewOpener
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Font
import java.util.concurrent.atomic.AtomicLong
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import kotlinx.coroutines.runBlocking

internal class RepoIssuesTreePanel(
    private val project: Project,
    private val provider: GitHubProvider,
    private val target: RepoIssueTarget,
    private val previewOpener: IssueEditorPreviewOpener
) {

    private val rootNode = DefaultMutableTreeNode("GitHub Issues")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel)
    private val status = JBLabel()
    private val sortBox = JComboBox<IssueSortOption>(IssueSortOption.entries.toTypedArray())
    private val openIssueButton = JButton("Open Issue")
    private val openRepoButton = JButton("Open Repo")
    private val treeRequests = AtomicLong()
    private var selectedIssue: Issue? = null

    val component: JComponent = createComponent()

    init {
        configureTree()
        reloadIssues()
    }

    private fun createComponent(): JComponent {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.add(createHeader(), BorderLayout.NORTH)
        panel.add(JBScrollPane(tree), BorderLayout.CENTER)
        return panel
    }

    private fun createHeader(): JComponent {
        val header = JBPanel<JBPanel<*>>()
        header.layout = BoxLayout(header, BoxLayout.Y_AXIS)
        header.border = JBUI.Borders.emptyBottom(8)

        val title = JBLabel("GitHub Issues").apply {
            font = font.deriveFont(Font.BOLD)
            alignmentX = Component.LEFT_ALIGNMENT
        }
        header.add(title)

        val sortRow = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), JBUI.scale(4))).apply {
            alignmentX = Component.LEFT_ALIGNMENT
        }
        val actionsRow = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
            alignmentX = Component.LEFT_ALIGNMENT
        }
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
            isEnabled = target.issuesUrl.isNotBlank()
            addActionListener { BrowserUtil.browse(target.issuesUrl) }
        }
        sortBox.apply {
            addActionListener { reloadIssues() }
        }

        sortRow.add(status)
        sortRow.add(JBLabel("Sort:"))
        sortRow.add(sortBox)
        actionsRow.add(refresh)
        actionsRow.add(openIssueButton)
        actionsRow.add(openRepoButton)
        header.add(sortRow)
        header.add(actionsRow)
        return header
    }

    private fun configureTree(): Unit {
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.cellRenderer = RepoIssuesTreeRenderer()
        tree.emptyText.text = "No GitHub issues"
        tree.selectionModel.selectionMode = javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.addTreeSelectionListener {
            handleSelection(tree.selectionPath)
        }
    }

    private fun reloadIssues(): Unit {
        val requestId = treeRequests.incrementAndGet()
        val sortOption = selectedSortOption()
        previewOpener.cancelPendingLoad()
        selectedIssue = null
        syncButtons()
        status.text = "Loading..."
        status.foreground = UIUtil.getContextHelpForeground()
        showLoadingNode()

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching {
                runBlocking {
                    val issues = provider.getIssuesSorted(
                        owner = target.owner,
                        repo = target.repoName,
                        state = "open",
                        filter = null,
                        labels = null,
                        sort = sortOption.apiSort,
                        direction = sortOption.apiDirection
                    )
                    val sortedIssues = sortOption.sort(issues)
                    val milestones = provider.getMilestones(target.owner, target.repoName)
                    val relationships = provider.getIssueRelationships(
                        owner = target.owner,
                        repo = target.repoName,
                        issues = sortedIssues
                    )
                    RepoLoadResult.Loaded(
                        issues = sortedIssues,
                        milestones = milestones,
                        relationships = relationships
                    )
                }
            }.getOrElse { error ->
                RepoLoadResult.Failed(error.message ?: "GitHub API request failed")
            }

            SwingUtilities.invokeLater {
                if (project.isDisposed || treeRequests.get() != requestId) return@invokeLater
                showIssueNodes(result)
            }
        }
    }

    private fun showLoadingNode(): Unit {
        rootNode.removeAllChildren()
        rootNode.add(DefaultMutableTreeNode(RepoIssueTreeItem.Message("Loading issues...")))
        treeModel.reload()
    }

    private fun showIssueNodes(result: RepoLoadResult): Unit {
        rootNode.removeAllChildren()
        when (result) {
            is RepoLoadResult.Loaded -> {
                val groups = IssueTreeGrouping.group(
                    milestones = result.milestones,
                    issues = result.issues,
                    relationships = result.relationships
                )
                if (groups.isEmpty()) {
                    rootNode.add(DefaultMutableTreeNode(RepoIssueTreeItem.Message("No open issues")))
                } else {
                    groups.forEach { milestone ->
                        val milestoneNode = DefaultMutableTreeNode(
                            RepoIssueTreeItem.Milestone(
                                target = target,
                                title = milestone.title,
                                openIssueCount = milestone.openIssueCount
                            )
                        )
                        if (milestone.rows.isEmpty()) {
                            milestoneNode.add(DefaultMutableTreeNode(RepoIssueTreeItem.Message("No open issues")))
                        } else {
                            milestone.rows.forEach { row ->
                                milestoneNode.add(createIssueNode(row))
                            }
                        }
                        rootNode.add(milestoneNode)
                    }
                }
                status.text = "${result.issues.size} open"
                status.foreground = UIUtil.getContextHelpForeground()
            }
            is RepoLoadResult.Failed -> {
                rootNode.add(DefaultMutableTreeNode(RepoIssueTreeItem.Message(result.message)))
                status.text = "Failed"
                status.foreground = UIUtil.getErrorForeground()
            }
        }
        treeModel.reload()
        syncButtons()
    }

    private fun handleSelection(path: TreePath?): Unit {
        val item = (path?.lastPathComponent as? DefaultMutableTreeNode)?.userObject
        when (item) {
            is RepoIssueTreeItem.Milestone -> {
                selectedIssue = null
                previewOpener.cancelPendingLoad()
            }
            is RepoIssueTreeItem.SelectableIssue -> {
                selectedIssue = item.issue
                previewOpener.openIssue(item.target, item.issue)
            }
            else -> selectedIssue = null
        }
        syncButtons()
    }

    private fun createIssueNode(row: IssueTreeGrouping.IssueRow): DefaultMutableTreeNode =
        when (row) {
            is IssueTreeGrouping.IssueRow.Parent -> {
                DefaultMutableTreeNode(RepoIssueTreeItem.ParentIssue(target, row.issue)).apply {
                    row.children.forEach { child ->
                        add(DefaultMutableTreeNode(RepoIssueTreeItem.SubIssue(target, child)))
                    }
                }
            }
            is IssueTreeGrouping.IssueRow.Standalone ->
                DefaultMutableTreeNode(RepoIssueTreeItem.StandaloneIssue(target, row.issue))
        }

    private fun selectedSortOption(): IssueSortOption =
        sortBox.selectedItem as? IssueSortOption ?: IssueSortOption.UPDATED_DESC

    private fun syncButtons(): Unit {
        openIssueButton.isEnabled = selectedIssue != null
    }

    private sealed interface RepoLoadResult {
        data class Loaded(
            val issues: List<Issue>,
            val milestones: List<IssueMilestone>,
            val relationships: List<IssueRelationship>
        ) : RepoLoadResult

        data class Failed(
            val message: String
        ) : RepoLoadResult
    }
}
