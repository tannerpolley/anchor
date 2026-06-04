package com.itsjeel01.remotevcsmanager.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.itsjeel01.remotevcsmanager.GitRemoteDetector
import com.itsjeel01.remotevcsmanager.models.*
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.settings.SettingsChangeNotifier
import com.itsjeel01.remotevcsmanager.ui.components.StateBadge
import com.itsjeel01.remotevcsmanager.ui.detail.IssueDetailPanel
import kotlinx.coroutines.runBlocking
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import kotlin.concurrent.thread

class RemoteVcsToolWindowPanel(project: Project) : SimpleToolWindowPanel(true, true) {

    private val gitDetector = GitRemoteDetector(project)
    private val provider = GitHubProvider()
    private val myProject = project

    private var remoteOwner: String? = null
    private var remoteRepo: String? = null
    private var remoteDetected = false

    // Header
    private val repoLabel = JBLabel("No remote").apply { font = JBUI.Fonts.label(13f).asBold() }
    private val branchLabel = JBLabel().apply { font = JBUI.Fonts.smallFont(); foreground = UIUtil.getContextHelpForeground() }
    private val statusLabel = JBLabel("Ready").apply { font = JBUI.Fonts.smallFont(); foreground = UIUtil.getContextHelpForeground() }

    // Navigation state
    private val cardPanel = JPanel(CardLayout())
    private val listPanel = JPanel(BorderLayout())
    private val detailContainer = JPanel(BorderLayout())
    private val cards: CardLayout get() = cardPanel.layout as CardLayout

    // Filters
    private val issueFilter = JComboBox(arrayOf("All", "Open", "Closed"))
    private val prFilter = JComboBox(arrayOf("Open", "Merged", "Closed", "All"))

    // Tabs
    private val tabPane = JTabbedPane().apply { font = JBUI.Fonts.label() }

    // Physical Panel Lists (Replaces JBList to fix hover/click bugs)
    private val issueListPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val prListPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val branchListPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

    // Data Caches
    private var issueData: List<Issue> = emptyList()
    private var prData: List<PullRequest> = emptyList()
    private var branchData: List<GitBranch> = emptyList()

    init {
        buildUI()
        regKeys()
        reloadConfig()

        val connection = ApplicationManager.getApplication().messageBus.connect()
        connection.subscribe(SettingsChangeNotifier.SETTINGS_CHANGED,
            SettingsChangeNotifier.SettingsChangeListener { reloadConfig() })
    }

    companion object {
        const val LIST = "list"
        const val DETAIL = "detail"
        val SF = JBUI.Fonts.smallFont()

        fun fmt(iso: String): String = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val d = sdf.parse(iso.take(19)) ?: return iso
            val diff = Date().time - d.time; val m = diff / 60000; val h = m / 60; val dd = h / 24; val w = dd / 7
            when { m < 1 -> "now"; m < 60 -> "${m}m"; h < 24 -> "${h}h"; dd < 7 -> "${dd}d"; w < 5 -> "${w}w"; else -> SimpleDateFormat("MMM d", Locale.US).format(d) }
        } catch (_: Exception) { iso }
    }

    private fun buildUI() {
        // Toolbar
        val tg = DefaultActionGroup().apply {
            add(object : AnAction("Refresh (R)", "Reload", AllIcons.Actions.Refresh) { override fun actionPerformed(e: AnActionEvent) = refresh(); override fun update(e: AnActionEvent) { e.presentation.isEnabled = true } })
            addSeparator()
            add(object : AnAction("New Issue", "Create Issue", AllIcons.General.Add) { override fun actionPerformed(e: AnActionEvent) = createIssue(); override fun update(e: AnActionEvent) { e.presentation.isEnabled = remoteDetected && provider.isConfigured() } })
            add(object : AnAction("New PR", "Create Pull Request", AllIcons.Vcs.Merge) { override fun actionPerformed(e: AnActionEvent) = createPR(); override fun update(e: AnActionEvent) { e.presentation.isEnabled = remoteDetected && provider.isConfigured() } })
            addSeparator()
            add(object : AnAction("Open Repo", "", AllIcons.Ide.External_link_arrow) { override fun actionPerformed(e: AnActionEvent) { val o = remoteOwner ?: return; val r = remoteRepo ?: return; BrowserUtil.browse("https://github.com/$o/$r") }; override fun update(e: AnActionEvent) { e.presentation.isEnabled = remoteDetected } })
            addSeparator()
            add(object : AnAction("Settings", "", AllIcons.General.Settings) { override fun actionPerformed(e: AnActionEvent) { com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(myProject, "Remote VCS Manager") } })
        }
        val tb = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, tg, true); tb.targetComponent = this
        add(JPanel(BorderLayout()).apply { add(tb.component, BorderLayout.WEST); add(statusLabel, BorderLayout.CENTER); border = JBUI.Borders.empty(1, 4) }, BorderLayout.NORTH)

        // Header (Aligned to HTML mockup)
        add(JPanel(BorderLayout()).apply {
            background = UIUtil.getTableBackground()
            border = BorderFactory.createCompoundBorder(JBUI.Borders.customLineBottom(UIUtil.getBoundsColor()), JBUI.Borders.empty(10, 12))
            add(JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply { isOpaque = false
                add(JLabel(AllIcons.Vcs.Branch).apply {
                    isOpaque = true; background = Color(107, 138, 253, 30); border = JBUI.Borders.empty(4)
                })
                add(repoLabel)
                add(branchLabel)
            }, BorderLayout.WEST)
        }, BorderLayout.BEFORE_FIRST_LINE)

        // Issue Tab
        issueFilter.addActionListener { renderIssues() }
        val issueScroll = JBScrollPane(issueListPanel).apply { border = JBUI.Borders.empty(); verticalScrollBar.unitIncrement = 16 }
        tabPane.addTab("Issues", AllIcons.General.TodoDefault, JPanel(BorderLayout()).apply {
            add(JPanel(FlowLayout(FlowLayout.LEFT, 4, 6)).apply { isOpaque = false; add(JBLabel("State:").apply { font = SF }); add(issueFilter) }, BorderLayout.NORTH)
            add(issueScroll, BorderLayout.CENTER)
        })

        // PR Tab
        prFilter.addActionListener { renderPRs() }
        val prScroll = JBScrollPane(prListPanel).apply { border = JBUI.Borders.empty(); verticalScrollBar.unitIncrement = 16 }
        tabPane.addTab("PRs", AllIcons.Vcs.Merge, JPanel(BorderLayout()).apply {
            add(JPanel(FlowLayout(FlowLayout.LEFT, 4, 6)).apply { isOpaque = false; add(JBLabel("State:").apply { font = SF }); add(prFilter) }, BorderLayout.NORTH)
            add(prScroll, BorderLayout.CENTER)
        })

        // Branch Tab
        val branchScroll = JBScrollPane(branchListPanel).apply { border = JBUI.Borders.empty(); verticalScrollBar.unitIncrement = 16 }
        tabPane.addTab("Branches", AllIcons.Vcs.Branch, branchScroll)

        // Setup CardLayout
        listPanel.add(tabPane, BorderLayout.CENTER)
        cardPanel.add(listPanel, LIST)
        cardPanel.add(detailContainer, DETAIL)
        cards.show(cardPanel, LIST)
        add(cardPanel, BorderLayout.CENTER)
    }

    private fun regKeys() {
        registerKeyboardAction({ refresh() }, KeyStroke.getKeyStroke("R"), WHEN_IN_FOCUSED_WINDOW)
        registerKeyboardAction({ if (detailContainer.isVisible) cards.show(cardPanel, LIST) }, KeyStroke.getKeyStroke("ESCAPE"), WHEN_IN_FOCUSED_WINDOW)
    }

    // ── Physical Rendering (Replaces ListCellRenderer) ───────────────────

    private fun renderIssues() {
        issueListPanel.removeAll()
        val f = issueFilter.selectedItem as? String ?: "All"
        val filtered = issueData.filter { when (f) { "Open" -> it.state == IssueState.OPEN; "Closed" -> it.state == IssueState.CLOSED; else -> true } }

        if (filtered.isEmpty()) {
            issueListPanel.add(JBLabel("No issues found", SwingConstants.CENTER).apply { border = JBUI.Borders.empty(20); foreground = UIUtil.getContextHelpForeground() })
        } else {
            filtered.forEach { issue -> issueListPanel.add(createIssueRow(issue)) }
        }
        issueListPanel.revalidate(); issueListPanel.repaint()
    }

    private fun createIssueRow(issue: Issue): JPanel {
        val row = JPanel(GridBagLayout()).apply {
            background = UIUtil.getTableBackground()
            border = BorderFactory.createCompoundBorder(JBUI.Borders.customLineBottom(UIUtil.getBoundsColor()), JBUI.Borders.empty(8, 12))
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }

        val badge = StateBadge().apply { setIssueState(issue.state) }
        val title = JBLabel("#${issue.number} ${issue.title}").apply { font = JBUI.Fonts.label() }
        val meta = JBLabel("by ${issue.author} · ${fmt(issue.updatedAt)}").apply { font = SF; foreground = UIUtil.getContextHelpForeground() }

        // Hover Actions
        val actionsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply { isOpaque = false; isVisible = false }
        val openBtn = flatIconBtn(AllIcons.Ide.External_link_arrow, "Open in browser") { issue.url?.let { BrowserUtil.browse(it) } }
        val copyBtn = flatIconBtn(AllIcons.Actions.Copy, "Copy link") { issue.url?.let { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(it), null) } }
        actionsPanel.add(copyBtn); actionsPanel.add(openBtn)

        row.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                row.background = UIUtil.getTableSelectionBackground(false)
                actionsPanel.isVisible = true
            }
            override fun mouseExited(e: MouseEvent) {
                row.background = UIUtil.getTableBackground()
                actionsPanel.isVisible = false
            }
            override fun mouseClicked(e: MouseEvent) { showDetail(buildIssueDetail(issue)) }
        })

        val c = GridBagConstraints()
        c.anchor = GridBagConstraints.WEST; c.gridx = 0; c.weightx = 0.0; c.insets = JBUI.insetsRight(8); row.add(badge, c)
        c.gridx = 1; c.weightx = 1.0; c.insets = JBUI.insets(0); row.add(title, c)
        c.gridx = 2; c.weightx = 0.0; c.insets = JBUI.insets(0); row.add(actionsPanel, c)
        c.gridx = 0; c.gridy = 1; c.gridwidth = 3; c.weightx = 0.0; c.insets = JBUI.insetsTop(4); row.add(meta, c)

        return row
    }

    private fun renderPRs() {
        prListPanel.removeAll()
        val f = prFilter.selectedItem as? String ?: "Open"
        val filtered = prData.filter { when (f) { "Open" -> it.state == PRState.OPEN; "Merged" -> it.state == PRState.MERGED; "Closed" -> it.state == PRState.CLOSED; else -> true } }

        filtered.forEach { pr -> prListPanel.add(createPRRow(pr)) }
        prListPanel.revalidate(); prListPanel.repaint()
    }

    private fun createPRRow(pr: PullRequest): JPanel {
        val row = JPanel(GridBagLayout()).apply {
            background = UIUtil.getTableBackground()
            border = BorderFactory.createCompoundBorder(JBUI.Borders.customLineBottom(UIUtil.getBoundsColor()), JBUI.Borders.empty(8, 12))
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }
        val badge = StateBadge().apply { setPRState(pr.state) }
        val title = JBLabel("#${pr.id} ${pr.title}").apply { font = JBUI.Fonts.label() }
        val meta = JBLabel("${pr.sourceBranch} → ${pr.targetBranch} · ${fmt(pr.updatedAt)}").apply { font = SF; foreground = UIUtil.getContextHelpForeground() }

        row.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) { row.background = UIUtil.getTableSelectionBackground(false) }
            override fun mouseExited(e: MouseEvent) { row.background = UIUtil.getTableBackground() }
            override fun mouseClicked(e: MouseEvent) { showDetail(buildPRDetail(pr)) }
        })

        val c = GridBagConstraints()
        c.anchor = GridBagConstraints.WEST; c.gridx = 0; c.weightx = 0.0; c.insets = JBUI.insetsRight(8); row.add(badge, c)
        c.gridx = 1; c.weightx = 1.0; c.insets = JBUI.insets(0); row.add(title, c)
        c.gridx = 0; c.gridy = 1; c.gridwidth = 2; c.weightx = 0.0; c.insets = JBUI.insetsTop(4); row.add(meta, c)
        return row
    }

    private fun renderBranches() {
        branchListPanel.removeAll()
        branchData.forEach { branch -> branchListPanel.add(createBranchRow(branch)) }
        branchListPanel.revalidate(); branchListPanel.repaint()
    }

    private fun createBranchRow(branch: GitBranch): JPanel {
        val row = JPanel(GridBagLayout()).apply {
            background = UIUtil.getTableBackground()
            border = BorderFactory.createCompoundBorder(JBUI.Borders.customLineBottom(UIUtil.getBoundsColor()), JBUI.Borders.empty(6, 12))
        }
        val name = JBLabel(branch.name).apply { font = Font(Font.MONOSPACED, Font.PLAIN, 12) }
        val sha = JBLabel(branch.sha.take(7)).apply { font = Font(Font.MONOSPACED, Font.PLAIN, 10); foreground = UIUtil.getContextHelpForeground() }

        val actionsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply { isOpaque = false; isVisible = false }
        actionsPanel.add(flatIconBtn(AllIcons.Vcs.Branch, "Checkout") { checkout(branch.name) })

        row.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) { row.background = UIUtil.getTableSelectionBackground(false); actionsPanel.isVisible = true }
            override fun mouseExited(e: MouseEvent) { row.background = UIUtil.getTableBackground(); actionsPanel.isVisible = false }
            override fun mouseClicked(e: MouseEvent) { showDetail(buildBranchDetail(branch)) }
        })

        val c = GridBagConstraints()
        if (branch.isDefault) { c.gridx = 0; c.weightx = 0.0; c.insets = JBUI.insetsRight(6); c.anchor = GridBagConstraints.WEST; row.add(JLabel(AllIcons.Vcs.Branch), c) }
        c.gridx = if (branch.isDefault) 1 else 0; c.weightx = 1.0; c.anchor = GridBagConstraints.WEST; row.add(name, c)
        c.gridx = 2; c.weightx = 0.0; c.anchor = GridBagConstraints.EAST; row.add(actionsPanel, c)
        c.gridx = 3; c.weightx = 0.0; c.anchor = GridBagConstraints.EAST; c.insets = JBUI.insetsLeft(8); row.add(sha, c)
        return row
    }

    // ── Navigation & Actions ──────────────────────────────────────────────

    private fun showDetail(c: JComponent) {
        detailContainer.removeAll()
        val back = JBLabel("  \u2190  Back").apply {
            font = SF; foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.empty(8, 12)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { cards.show(cardPanel, LIST) }
                override fun mouseEntered(e: MouseEvent) { foreground = JBUI.CurrentTheme.Link.Foreground.HOVERED }
                override fun mouseExited(e: MouseEvent) { foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED }
            })
        }
        detailContainer.add(JPanel(BorderLayout()).apply { background = UIUtil.getTableBackground(); border = JBUI.Borders.customLineBottom(UIUtil.getBoundsColor()); add(back, BorderLayout.WEST) }, BorderLayout.NORTH)
        detailContainer.add(c, BorderLayout.CENTER)
        cards.show(cardPanel, DETAIL)
    }

    private fun buildIssueDetail(i: Issue) = IssueDetailPanel(provider, remoteOwner ?: "", remoteRepo ?: "", i, onBack = { cards.show(cardPanel, LIST) }, onRefresh = { refresh() })

    private fun buildPRDetail(p: PullRequest): JComponent {
        val panel = JPanel(BorderLayout()).apply { border = JBUI.Borders.empty(12) }
        panel.add(JPanel(BorderLayout()).apply { isOpaque = false; add(StateBadge().apply { setPRState(p.state) }, BorderLayout.WEST); add(JBLabel("  #${p.id} ${p.title}").apply { font = JBUI.Fonts.label(14f).asBold() }, BorderLayout.CENTER) }, BorderLayout.NORTH)
        panel.add(JBLabel("${p.sourceBranch} → ${p.targetBranch} · ${fmt(p.updatedAt)}").apply { font = SF; foreground = UIUtil.getContextHelpForeground(); border = JBUI.Borders.emptyTop(8) }, BorderLayout.CENTER)
        panel.add(JButton("Open in Browser", AllIcons.Ide.External_link_arrow).apply { addActionListener { BrowserUtil.browse(p.url) } }, BorderLayout.SOUTH)
        return panel
    }

    private fun buildBranchDetail(b: GitBranch): JComponent {
        val panel = JPanel(BorderLayout()).apply { border = JBUI.Borders.empty(12) }
        panel.add(JBLabel(b.name).apply { font = JBUI.Fonts.label(14f).asBold() }, BorderLayout.NORTH)
        val acts = JPanel(FlowLayout(FlowLayout.LEFT, 4, 8)).apply { isOpaque = false
            add(JButton("Checkout", AllIcons.Vcs.Branch).apply { addActionListener { checkout(b.name) } })
            add(JButton(AllIcons.Ide.External_link_arrow).apply { addActionListener { BrowserUtil.browse("https://github.com/${remoteOwner}/${remoteRepo}/tree/${b.name}") } })
        }
        panel.add(acts, BorderLayout.SOUTH)
        return panel
    }

    fun reloadConfig() {
        remoteDetected = gitDetector.hasRemote() && provider.isConfigured()
        if (remoteDetected) {
            val info = gitDetector.detect()
            if (info != null) { remoteOwner = info.owner; remoteRepo = info.repoName; repoLabel.text = "${info.owner}/${info.repoName}"; branchLabel.text = "  ·  ${info.currentBranch ?: "unknown"}" }
            refresh()
        } else {
            repoLabel.text = if (!gitDetector.hasRemote()) "No remote" else "No token"
            branchLabel.text = ""
            statusLabel.text = if (!gitDetector.hasRemote()) "Set a git remote first" else "Configure token in Settings"
            issueData = emptyList(); prData = emptyList(); branchData = emptyList()
            renderIssues(); renderPRs(); renderBranches()
        }
    }

    fun refresh() {
        if (remoteOwner == null || remoteRepo == null) { reloadConfig(); return }
        cards.show(cardPanel, LIST); statusLabel.text = "Loading..."; statusLabel.foreground = UIUtil.getContextHelpForeground()

        thread {
            try {
                val issues = runBlocking { provider.getIssues(remoteOwner!!, remoteRepo!!, "open", null, null) }
                val closed = runBlocking { provider.getIssues(remoteOwner!!, remoteRepo!!, "closed", null, null) }
                val prs = runBlocking { provider.getPullRequests(remoteOwner!!, remoteRepo!!, "open") }
                val branches = runBlocking { provider.getBranches(remoteOwner!!, remoteRepo!!) }
                SwingUtilities.invokeLater {
                    issueData = issues + closed; renderIssues()
                    prData = prs; renderPRs()
                    branchData = branches; renderBranches()

                    tabPane.setTitleAt(0, "Issues (${issues.size})")
                    tabPane.setTitleAt(1, "PRs (${prs.size})")
                    tabPane.setTitleAt(2, "Branches (${branches.size})")
                    statusLabel.text = "Synced successfully"; statusLabel.foreground = UIUtil.getActiveTextColor()
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater { statusLabel.text = "Error syncing data"; statusLabel.foreground = UIUtil.getErrorForeground()
                    JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(e.message ?: "Failed to fetch data", MessageType.ERROR, null).setTitle("Sync Failed").createBalloon().show(JBPopupFactory.getInstance().guessBestPopupLocation(tabPane), Balloon.Position.below) }
            }
        }
    }

    private fun checkout(name: String) {
        try { val r = gitDetector.detect()?.gitRoot ?: return; Runtime.getRuntime().exec(arrayOf("git", "checkout", name), null, r); JOptionPane.showMessageDialog(null, "Switched to $name") }
        catch (e: Exception) { JOptionPane.showMessageDialog(null, "Failed: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE) }
    }

    private fun createIssue() {
        val o = remoteOwner ?: return; val r = remoteRepo ?: return
        CreateIssueDialog(myProject, o, r, provider).showAndGet()
        refresh()
    }

    private fun createPR() {
        val o = remoteOwner ?: return; val r = remoteRepo ?: return
        // Fallback to browser if API PR creation isn't fully robust, or pop a dialog
        // This opens the compare page on GitHub which is standard for most JetBrains plugins handling PRs
        BrowserUtil.browse("https://github.com/$o/$r/compare")
    }

    private fun flatIconBtn(icon: javax.swing.Icon, tip: String, action: () -> Unit) = JButton(icon).apply {
        isOpaque = false; border = JBUI.Borders.empty(4); isContentAreaFilled = false; toolTipText = tip;
        addActionListener { action() }
    }
}

class CreateIssueDialog(project: Project?, private val owner: String, private val repo: String, private val provider: GitHubProvider) : com.intellij.openapi.ui.DialogWrapper(project) {
    private val tf = JBTextField().apply { emptyText.text = "Issue title" }
    private val ta = JBTextArea().apply { emptyText.text = "Leave a description..."; lineWrap = true; rows = 6; font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.Fonts.label().size) }
    init { title = "New Issue — $owner/$repo"; init() }
    override fun createCenterPanel(): JComponent = JPanel(BorderLayout()).apply {
        add(com.intellij.util.ui.FormBuilder.createFormBuilder()
            .addLabeledComponent("Title:", tf, true).addVerticalGap(8)
            .addLabeledComponent("Description:", JBScrollPane(ta), true).panel.apply { border = JBUI.Borders.empty(8); preferredSize = Dimension(500, 300) }, BorderLayout.CENTER)
    }
    override fun doOKAction() {
        val t = tf.text.trim()
        if (t.isBlank()) { com.intellij.openapi.ui.Messages.showErrorDialog("Title is required to create an issue.", "Validation Error"); return }
        ProgressManager.getInstance().run(object : com.intellij.openapi.progress.Task.Backgroundable(null, "Pushing issue...", true) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                indicator.isIndeterminate = true
                try {
                    runBlocking { provider.createIssue(owner, repo, t, ta.text.ifBlank { null }) }
                    ApplicationManager.getApplication().invokeLater { this@CreateIssueDialog.close(OK_EXIT_CODE) }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater { com.intellij.openapi.ui.Messages.showErrorDialog("Failed: ${e.message}", "Error") }
                }
            }
        })
    }
}