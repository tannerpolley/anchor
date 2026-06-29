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
    private val allTargets: List<RepoIssueTarget>,
    initialInclusionState: RepoIssueInclusionState,
    private val onInclusionChanged: (RepoIssueInclusionState) -> Unit,
    private val accountLogins: Set<String>,
    private val previewOpener: IssueEditorPreviewOpener
) {

    private val rootNode = DefaultMutableTreeNode("GitHub Issues")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel)
    private val status = JBLabel()
    private val sortBox = JComboBox<IssueSortOption>(IssueSortOption.entries.toTypedArray())
    private val reposButton = JButton("Repos")
    private val openIssueButton = JButton("Open Issue")
    private val openRepoButton = JButton("Open Repo")
    private val treeRequests = AtomicLong()
    private var inclusionState = initialInclusionState
    private var targets: List<RepoIssueTarget> = inclusionState.includedTargets(allTargets)
    private var selectedTarget: RepoIssueTarget? = targets.firstOrNull() ?: allTargets.firstOrNull()
    private var selectedIssue: Issue? = null
    private var hasVisibleTargets = targets.isNotEmpty()

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

        val titleRow = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            alignmentX = Component.LEFT_ALIGNMENT
        }
        val title = JBLabel("GitHub Issues").apply {
            font = font.deriveFont(Font.BOLD)
        }
        titleRow.add(title, BorderLayout.WEST)
        header.add(titleRow)

        val actions = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
            alignmentX = Component.LEFT_ALIGNMENT
        }
        val refresh = JButton("Refresh").apply {
            addActionListener { reloadIssues() }
        }
        reposButton.apply {
            addActionListener { showRepositorySelectionDialog() }
        }
        openIssueButton.apply {
            isEnabled = false
            addActionListener {
                selectedIssue?.let { BrowserUtil.browse(it.url) }
            }
        }
        openRepoButton.apply {
            addActionListener { selectedTarget?.let { BrowserUtil.browse(it.issuesUrl) } }
        }
        sortBox.apply {
            addActionListener { reloadIssues() }
        }

        actions.add(status)
        actions.add(JBLabel("Sort:"))
        actions.add(sortBox)
        actions.add(reposButton)
        actions.add(refresh)
        actions.add(openIssueButton)
        actions.add(openRepoButton)
        header.add(actions)
        return header
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
        val sortOption = selectedSortOption()
        previewOpener.cancelPendingLoad()
        selectedIssue = null
        syncButtons()
        if (targets.isEmpty()) {
            hasVisibleTargets = false
            status.text = "0 selected"
            status.foreground = UIUtil.getContextHelpForeground()
            showNoSelectedNodes()
            syncButtons()
            return
        }
        status.text = "Loading ${targets.size} repos..."
        status.foreground = UIUtil.getContextHelpForeground()
        showLoadingNodes()

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = targets.map { target ->
                runCatching {
                    runBlocking {
                        val access = provider.getIssueTrackingAccess(target.owner, target.repoName, accountLogins)
                        when {
                            access.fork -> RepoLoadResult.HiddenFork(target)
                            !access.owned -> RepoLoadResult.HiddenNotOwned(target)
                            else -> {
                                val issues = provider.getIssuesSorted(
                                    owner = target.owner,
                                    repo = target.repoName,
                                    state = "open",
                                    filter = null,
                                    labels = null,
                                    sort = sortOption.apiSort,
                                    direction = sortOption.apiDirection
                                )
                                RepoLoadResult.Loaded(target, sortOption.sort(issues))
                            }
                        }
                    }
                }.getOrElse { error ->
                    RepoLoadResult.Failed(target, error.message ?: "GitHub API request failed")
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

    private fun showIssueNodes(results: List<RepoLoadResult>): Unit {
        rootNode.removeAllChildren()
        var total = 0
        var failures = 0
        var hiddenForks = 0
        var hiddenNonOwned = 0
        val visibleTargets = mutableListOf<RepoIssueTarget>()

        results.forEach { result ->
            when (result) {
                is RepoLoadResult.Loaded -> {
                    val target = result.target
                    val issues = result.issues
                    visibleTargets.add(target)
                    total += issues.size
                    val repoNode = DefaultMutableTreeNode(RepoIssueTreeItem.Repository(target, issues.size))
                    if (issues.isEmpty()) {
                        repoNode.add(DefaultMutableTreeNode(RepoIssueTreeItem.Message("No open issues")))
                    } else {
                        IssueMilestoneGrouping.group(issues).forEach { milestone ->
                            val milestoneNode = DefaultMutableTreeNode(
                                RepoIssueTreeItem.Milestone(
                                    target = target,
                                    title = milestone.title,
                                    openIssueCount = milestone.issues.size
                                )
                            )
                            milestone.issues.forEach { issue ->
                                milestoneNode.add(DefaultMutableTreeNode(RepoIssueTreeItem.IssueNode(target, issue)))
                            }
                            repoNode.add(milestoneNode)
                        }
                    }
                    rootNode.add(repoNode)
                }
                is RepoLoadResult.Failed -> {
                    failures += 1
                    visibleTargets.add(result.target)
                    val repoNode = DefaultMutableTreeNode(RepoIssueTreeItem.Repository(result.target, null))
                    repoNode.add(
                        DefaultMutableTreeNode(
                            RepoIssueTreeItem.Message(result.message)
                        )
                    )
                    rootNode.add(repoNode)
                }
                is RepoLoadResult.HiddenFork -> hiddenForks += 1
                is RepoLoadResult.HiddenNotOwned -> hiddenNonOwned += 1
            }
        }

        if (rootNode.childCount == 0) {
            rootNode.add(DefaultMutableTreeNode(RepoIssueTreeItem.Message("No owned GitHub repositories to track")))
        }
        hasVisibleTargets = visibleTargets.isNotEmpty()
        if (selectedTarget == null || selectedTarget !in visibleTargets) {
            selectedTarget = visibleTargets.firstOrNull()
        }
        treeModel.reload()
        expandRepositoryNodes()
        status.text = statusText(total, failures, hiddenForks, hiddenNonOwned)
        status.foreground = if (failures == 0) UIUtil.getContextHelpForeground() else UIUtil.getErrorForeground()
        syncButtons()
    }

    private fun showRepositorySelectionDialog(): Unit {
        val dialog = RepositoryInclusionDialog(project, allTargets, inclusionState)
        if (!dialog.showAndGet()) return

        inclusionState = dialog.selectedState()
        onInclusionChanged(inclusionState)
        targets = inclusionState.includedTargets(allTargets)
        selectedTarget = targets.firstOrNull() ?: allTargets.firstOrNull()
        hasVisibleTargets = targets.isNotEmpty()
        reloadIssues()
    }

    private fun showNoSelectedNodes(): Unit {
        rootNode.removeAllChildren()
        rootNode.add(DefaultMutableTreeNode(RepoIssueTreeItem.Message("No repositories selected")))
        treeModel.reload()
    }

    private fun handleSelection(path: TreePath?): Unit {
        val item = (path?.lastPathComponent as? DefaultMutableTreeNode)?.userObject
        when (item) {
            is RepoIssueTreeItem.Repository -> {
                selectedTarget = item.target
                selectedIssue = null
                previewOpener.cancelPendingLoad()
            }
            is RepoIssueTreeItem.Milestone -> {
                selectedTarget = item.target
                selectedIssue = null
                previewOpener.cancelPendingLoad()
            }
            is RepoIssueTreeItem.IssueNode -> {
                selectedTarget = item.target
                selectedIssue = item.issue
                previewOpener.openIssue(item.target, item.issue)
            }
            else -> selectedIssue = null
        }
        syncButtons()
    }

    private fun expandRepositoryNodes(): Unit {
        val expandedFirstMilestones = mutableSetOf<String>()
        var row = 0
        while (row < tree.rowCount) {
            val path = tree.getPathForRow(row)
            val item = (path?.lastPathComponent as? DefaultMutableTreeNode)?.userObject
            when (item) {
                is RepoIssueTreeItem.Repository -> tree.expandPath(path)
                is RepoIssueTreeItem.Milestone -> {
                    if (expandedFirstMilestones.add(item.target.issuesUrl)) {
                        tree.expandPath(path)
                    }
                }
            }
            row += 1
        }
    }

    private fun statusText(total: Int, failures: Int, hiddenForks: Int, hiddenNonOwned: Int): String =
        listOfNotNull(
            "$total open",
            if (hiddenForks > 0) "$hiddenForks forks hidden" else null,
            if (hiddenNonOwned > 0) "$hiddenNonOwned not owned hidden" else null,
            if (failures > 0) "$failures failed" else null
        ).joinToString(", ")

    private fun selectedSortOption(): IssueSortOption =
        sortBox.selectedItem as? IssueSortOption ?: IssueSortOption.UPDATED_DESC

    private fun syncButtons(): Unit {
        openIssueButton.isEnabled = selectedIssue != null
        openRepoButton.isEnabled = hasVisibleTargets && selectedTarget?.issuesUrl?.isNotBlank() == true
    }

    private sealed interface RepoLoadResult {
        data class Loaded(
            val target: RepoIssueTarget,
            val issues: List<Issue>
        ) : RepoLoadResult

        data class Failed(
            val target: RepoIssueTarget,
            val message: String
        ) : RepoLoadResult

        data class HiddenFork(
            val target: RepoIssueTarget
        ) : RepoLoadResult

        data class HiddenNotOwned(
            val target: RepoIssueTarget
        ) : RepoLoadResult
    }
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

    data class Milestone(
        val target: RepoIssueTarget,
        val title: String,
        val openIssueCount: Int
    ) : RepoIssueTreeItem

    data class Message(
        val text: String
    ) : RepoIssueTreeItem
}
