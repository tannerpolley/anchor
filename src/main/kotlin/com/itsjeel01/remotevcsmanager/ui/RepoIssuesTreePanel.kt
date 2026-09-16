package com.itsjeel01.remotevcsmanager.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFrame
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueDependency
import com.itsjeel01.remotevcsmanager.models.IssueMilestone
import com.itsjeel01.remotevcsmanager.models.IssueRelationship
import com.itsjeel01.remotevcsmanager.models.IssueState
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.settings.RemoteVcsSettingsState
import com.itsjeel01.remotevcsmanager.settings.SettingsChangeNotifier
import com.itsjeel01.remotevcsmanager.ui.editor.IssueEditorPreviewOpener
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.HierarchyEvent
import java.util.concurrent.atomic.AtomicLong
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer
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
    private val refreshButton = JButton("Refresh")
    private val openIssueButton = JButton("Open Issue")
    private val openRepoButton = JButton("Open Repo")
    private val treeRequests = AtomicLong()
    private val settings = RemoteVcsSettingsState.getInstance()
    private val autoRefreshTimer = Timer(AUTO_REFRESH_INTERVAL_MS) {
        if (component.isShowing) reloadIssues(automatic = true)
    }
    private var hasTreeData = false
    private var isRefreshing = false
    private var restoringTreeState = false
    private var lastRefreshAttempt = 0L
    private var lastSuccessfulRefresh: String? = null
    private var selectedIssue: Issue? = null

    val component: JComponent = createComponent()

    init {
        configureTree()
        configureAutoRefresh()
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
        refreshButton.apply {
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
        actionsRow.add(refreshButton)
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

    private fun configureAutoRefresh(): Unit {
        component.addHierarchyListener { event ->
            if (
                event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L &&
                component.isShowing
            ) {
                reloadIssues(automatic = true)
            }
        }
        ApplicationManager.getApplication().messageBus.connect(project).apply {
            subscribe(
                ApplicationActivationListener.TOPIC,
                object : ApplicationActivationListener {
                    override fun applicationActivated(ideFrame: IdeFrame): Unit {
                        if (ideFrame.project == project && component.isShowing) {
                            reloadIssues(automatic = true)
                        }
                    }
                }
            )
            subscribe(
                SettingsChangeNotifier.SETTINGS_CHANGED,
                SettingsChangeNotifier.SettingsChangeListener {
                    if (component.isShowing) reloadIssues(automatic = true)
                }
            )
        }
        autoRefreshTimer.start()
        Disposer.register(project) { autoRefreshTimer.stop() }
    }

    private fun reloadIssues(automatic: Boolean = false): Unit {
        val now = System.currentTimeMillis()
        if (automatic && !settings.getAutoRefresh()) return
        if (automatic && now - lastRefreshAttempt < AUTO_REFRESH_THROTTLE_MS) return
        if (isRefreshing) return

        isRefreshing = true
        lastRefreshAttempt = now
        val requestId = treeRequests.incrementAndGet()
        val sortOption = selectedSortOption()
        syncButtons()
        status.text = lastSuccessfulRefresh?.let { "Refreshing... · Last updated $it" } ?: "Loading..."
        status.foreground = UIUtil.getContextHelpForeground()
        status.toolTipText = null
        if (!hasTreeData) showLoadingNode()

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
                    val dependencies = provider.getIssueDependencies(
                        owner = target.owner,
                        repo = target.repoName,
                        issues = sortedIssues
                    )
                    RepoLoadResult.Loaded(
                        issues = sortedIssues,
                        milestones = milestones,
                        relationships = relationships,
                        dependencies = dependencies
                    )
                }
            }.getOrElse { error ->
                RepoLoadResult.Failed(error.message ?: "GitHub API request failed")
            }

            SwingUtilities.invokeLater {
                if (project.isDisposed || treeRequests.get() != requestId) return@invokeLater
                isRefreshing = false
                when (result) {
                    is RepoLoadResult.Loaded -> {
                        val treeState = RepoIssueTreeStateKeeper.capture(
                            tree,
                            rootNode,
                            selectedIssue?.number
                        )
                        val selectedBeforeRefresh = selectedIssue
                        lastSuccessfulRefresh = TimeFormat.now()
                        showIssueNodes(result, treeState)
                        selectedBeforeRefresh?.let { previewOpener.refreshIssue(target, it) }
                    }
                    is RepoLoadResult.Failed -> showRefreshFailure(result.message)
                }
                syncButtons()
            }
        }
    }

    private fun showLoadingNode(): Unit {
        rootNode.removeAllChildren()
        rootNode.add(DefaultMutableTreeNode(RepoIssueTreeItem.Message("Loading issues...")))
        treeModel.reload()
    }

    private fun showIssueNodes(result: RepoLoadResult.Loaded, treeState: RepoIssueTreeState): Unit {
        rootNode.removeAllChildren()
        val groups = IssueTreeGrouping.group(
            milestones = result.milestones,
            issues = result.issues,
            relationships = result.relationships
        )
        val openBlockersByIssue = result.dependencies
            .filter { it.blockingIssue.state == IssueState.OPEN }
            .groupBy({ it.blockedIssueNumber }, { it.blockingIssue })
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
                        milestoneNode.add(createIssueNode(row, openBlockersByIssue))
                    }
                }
                rootNode.add(milestoneNode)
            }
        }
        hasTreeData = true
        status.text = "${result.issues.size} open · Updated ${lastSuccessfulRefresh.orEmpty()}"
        status.foreground = UIUtil.getContextHelpForeground()
        status.toolTipText = null
        treeModel.reload()
        restoringTreeState = true
        try {
            selectedIssue = RepoIssueTreeStateKeeper.restore(tree, rootNode, treeState)
        } finally {
            restoringTreeState = false
        }
    }

    private fun showRefreshFailure(message: String): Unit {
        status.text = lastSuccessfulRefresh?.let {
            "Refresh failed · Stale since $it"
        } ?: "Refresh failed"
        status.foreground = UIUtil.getErrorForeground()
        status.toolTipText = message
        if (!hasTreeData) {
            rootNode.removeAllChildren()
            rootNode.add(DefaultMutableTreeNode(RepoIssueTreeItem.Message(message)))
            treeModel.reload()
        }
    }

    private fun handleSelection(path: TreePath?): Unit {
        if (restoringTreeState) return
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

    private fun createIssueNode(
        row: IssueTreeGrouping.IssueRow,
        openBlockersByIssue: Map<Int, List<Issue>>
    ): DefaultMutableTreeNode =
        when (row) {
            is IssueTreeGrouping.IssueRow.Parent -> {
                DefaultMutableTreeNode(
                    RepoIssueTreeItem.ParentIssue(
                        target,
                        row.issue,
                        openBlockersByIssue[row.issue.number].orEmpty()
                    )
                ).apply {
                    row.children.forEach { child ->
                        add(
                            DefaultMutableTreeNode(
                                RepoIssueTreeItem.SubIssue(
                                    target,
                                    child,
                                    openBlockersByIssue[child.number].orEmpty()
                                )
                            )
                        )
                    }
                }
            }
            is IssueTreeGrouping.IssueRow.Standalone ->
                DefaultMutableTreeNode(
                    RepoIssueTreeItem.StandaloneIssue(
                        target,
                        row.issue,
                        openBlockersByIssue[row.issue.number].orEmpty()
                    )
                )
        }

    private fun selectedSortOption(): IssueSortOption =
        sortBox.selectedItem as? IssueSortOption ?: IssueSortOption.UPDATED_DESC

    private fun syncButtons(): Unit {
        openIssueButton.isEnabled = selectedIssue != null
        refreshButton.isEnabled = !isRefreshing
        sortBox.isEnabled = !isRefreshing
    }

    private sealed interface RepoLoadResult {
        data class Loaded(
            val issues: List<Issue>,
            val milestones: List<IssueMilestone>,
            val relationships: List<IssueRelationship>,
            val dependencies: List<IssueDependency>
        ) : RepoLoadResult

        data class Failed(
            val message: String
        ) : RepoLoadResult
    }

    companion object {
        private const val AUTO_REFRESH_INTERVAL_MS = 5 * 60 * 1000
        private const val AUTO_REFRESH_THROTTLE_MS = 60 * 1000
    }
}
