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
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.itsjeel01.remotevcsmanager.GitRemoteDetector
import com.itsjeel01.remotevcsmanager.models.*
import com.itsjeel01.remotevcsmanager.models.Label as VsLabel
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.settings.SettingsChangeNotifier
import com.itsjeel01.remotevcsmanager.ui.components.StateBadge
import com.itsjeel01.remotevcsmanager.ui.detail.IssueDetailPanel
import com.itsjeel01.remotevcsmanager.ui.detail.PullRequestDetailPanel
import kotlinx.coroutines.runBlocking
import java.awt.*
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

    // Navigation
    private val cardPanel = JPanel(CardLayout())
    private val listPanel = JPanel(BorderLayout())
    private val detailContainer = JPanel(BorderLayout())
    private val cards: CardLayout get() = cardPanel.layout as CardLayout

    // Filter state
    private var issueFilterState = "all"
    private var prFilterState = "open"

    // Tabs
    private val tabPane = JTabbedPane().apply { font = JBUI.Fonts.label() }

    // Lists
    private val issueListPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val prListPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val branchListPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

    // Data
    private var issueData: List<Issue> = emptyList()
    private var prData: List<PullRequest> = emptyList()
    private var branchData: List<GitBranch> = emptyList()

    // Chip labels (for in-place style updates — no panel rebuilding)
    private var issueChipLabels: MutableList<JBLabel> = mutableListOf()
    private var prChipLabels: MutableList<JBLabel> = mutableListOf()

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

    // ── Filter Chips (in-place updates, no panel rebuild) ─────────────────

    private fun makeChipRow(
        options: Map<String, String>,   // value → display label
        selected: String,
        chipLabels: MutableList<JBLabel>,
        onSelect: (String) -> Unit
    ): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 6)).apply { isOpaque = false }
        chipLabels.clear()

        options.forEach { (value, label) ->
            val chip = JBLabel(" $label ")
            chip.font = SF
            chip.isOpaque = true
            chip.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            chip.border = JBUI.Borders.empty(3, 8)
            chip.name = value // store option value in component name
            chip.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    updateChipSelection(chipLabels, chip)
                    onSelect(value)
                }
                override fun mouseEntered(e: MouseEvent) {
                    if (chip.background != UIUtil.getListSelectionBackground(true)) {
                        chip.background = UIUtil.getListSelectionBackground(false)
                    }
                }
                override fun mouseExited(e: MouseEvent) {
                    if (chip.background != UIUtil.getListSelectionBackground(true)) {
                        chip.background = UIUtil.getBoundsColor()
                    }
                }
            })
            chipLabels.add(chip)
            panel.add(chip)
        }

        // Apply initial selection
        updateChipSelection(chipLabels, chipLabels.firstOrNull { it.name == selected })
        return panel
    }

    private fun updateChipSelection(allChips: MutableList<JBLabel>, selectedChip: JBLabel?) {
        allChips.forEach { c ->
            val isSel = c == selectedChip
            c.foreground = if (isSel) UIUtil.getListSelectionForeground(true) else UIUtil.getLabelForeground()
            c.background = if (isSel) UIUtil.getListSelectionBackground(true) else UIUtil.getBoundsColor()
        }
    }

    // ── Build ──────────────────────────────────────────────────────────────

    private fun buildUI() {
        // Toolbar
        val tg = DefaultActionGroup().apply {
            add(object : AnAction("Refresh (R)", "Reload", AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) = refresh()
                override fun update(e: AnActionEvent) { e.presentation.isEnabled = true }
            })
            addSeparator()
            add(object : AnAction("New Issue", "Create Issue", AllIcons.General.Add) {
                override fun actionPerformed(e: AnActionEvent) = createIssue()
                override fun update(e: AnActionEvent) { e.presentation.isEnabled = remoteDetected && provider.isConfigured() }
            })
            add(object : AnAction("New PR", "Create Pull Request", AllIcons.Vcs.Merge) {
                override fun actionPerformed(e: AnActionEvent) = createPR()
                override fun update(e: AnActionEvent) { e.presentation.isEnabled = remoteDetected && provider.isConfigured() }
            })
            addSeparator()
            add(object : AnAction("Open Repo", "", AllIcons.Ide.External_link_arrow) {
                override fun actionPerformed(e: AnActionEvent) {
                    val o = remoteOwner ?: return; val r = remoteRepo ?: return
                    BrowserUtil.browse("https://github.com/$o/$r")
                }
                override fun update(e: AnActionEvent) { e.presentation.isEnabled = remoteDetected }
            })
            addSeparator()
            add(object : AnAction("Settings", "", AllIcons.General.Settings) {
                override fun actionPerformed(e: AnActionEvent) {
                    com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(myProject, "Remote VCS Manager")
                }
            })
        }
        val tb = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, tg, true); tb.targetComponent = this
        add(JPanel(BorderLayout()).apply {
            add(tb.component, BorderLayout.WEST)
            add(statusLabel, BorderLayout.CENTER)
            border = JBUI.Borders.empty(1, 4)
        }, BorderLayout.NORTH)

        // Header with repo info (left) and CTA action buttons (right) - JPanel for clean look
        val issueCta = JPanel(GridBagLayout()).apply {
            isOpaque = true; background = JBColor(0x3FB950, 0x2EA043)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.empty(4, 8)
            add(JLabel(AllIcons.General.Add).apply { foreground = Color.WHITE })
            add(JBLabel(" New Issue").apply { font = SF; foreground = Color.WHITE })
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { createIssue() }
                override fun mouseEntered(e: MouseEvent) { background = JBColor(0x3FB950, 0x2EA043).darker() }
                override fun mouseExited(e: MouseEvent) { background = JBColor(0x3FB950, 0x2EA043) }
            })
        }
        val prCta = JPanel(GridBagLayout()).apply {
            isOpaque = true; background = JBColor(0x539BF5, 0x316DCA)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.empty(4, 8)
            add(JLabel(AllIcons.Vcs.Merge).apply { foreground = Color.WHITE })
            add(JBLabel(" New PR").apply { font = SF; foreground = Color.WHITE })
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { createPR() }
                override fun mouseEntered(e: MouseEvent) { background = JBColor(0x539BF5, 0x316DCA).darker() }
                override fun mouseExited(e: MouseEvent) { background = JBColor(0x539BF5, 0x316DCA) }
            })
        }

        add(JPanel(GridBagLayout()).apply {
            background = UIUtil.getTableBackground()
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLineBottom(UIUtil.getBoundsColor()),
                JBUI.Borders.empty(8, 12)
            )
            val hgc = GridBagConstraints()
            hgc.anchor = GridBagConstraints.WEST; hgc.fill = GridBagConstraints.NONE
            hgc.gridx = 0; hgc.weightx = 0.0
            add(JLabel(AllIcons.Vcs.Branch).apply {
                isOpaque = true
                background = Color(107, 138, 253, 30)
                border = JBUI.Borders.empty(4)
            }, hgc)
            hgc.gridx = 1; hgc.insets = JBUI.insetsLeft(8)
            add(repoLabel, hgc)
            hgc.gridx = 2; hgc.insets = JBUI.insetsLeft(4)
            add(branchLabel, hgc)
            hgc.gridx = 3; hgc.weightx = 1.0; hgc.fill = GridBagConstraints.HORIZONTAL; hgc.insets = JBUI.emptyInsets()
            add(Box.createHorizontalGlue(), hgc)
            hgc.gridx = 4; hgc.weightx = 0.0; hgc.fill = GridBagConstraints.NONE; hgc.insets = JBUI.insetsRight(6)
            add(issueCta, hgc)
            hgc.gridx = 5; hgc.insets = JBUI.emptyInsets()
            add(prCta, hgc)
        }, BorderLayout.BEFORE_FIRST_LINE)

        // ── Issue Tab ──
        val issueScroll = JBScrollPane(issueListPanel).apply {
            border = JBUI.Borders.empty(); verticalScrollBar.unitIncrement = 16
        }
        val issueChipRow = makeChipRow(
            mapOf("all" to "All", "open" to "Open", "closed" to "Closed"),
            issueFilterState,
            issueChipLabels
        ) { opt ->
            issueFilterState = opt
            renderIssues()
        }
        tabPane.addTab("Issues", AllIcons.General.TodoDefault, JPanel(BorderLayout()).apply {
            add(issueChipRow, BorderLayout.NORTH)
            add(issueScroll, BorderLayout.CENTER)
        })

        // ── PR Tab ──
        val prScroll = JBScrollPane(prListPanel).apply {
            border = JBUI.Borders.empty(); verticalScrollBar.unitIncrement = 16
        }
        val prChipRow = makeChipRow(
            mapOf("open" to "Open", "merged" to "Merged", "closed" to "Closed", "all" to "All"),
            prFilterState,
            prChipLabels
        ) { opt ->
            prFilterState = opt
            renderPRs()
        }
        tabPane.addTab("PRs", AllIcons.Vcs.Merge, JPanel(BorderLayout()).apply {
            add(prChipRow, BorderLayout.NORTH)
            add(prScroll, BorderLayout.CENTER)
        })

        // ── Branch Tab ──
        val branchScroll = JBScrollPane(branchListPanel).apply {
            border = JBUI.Borders.empty(); verticalScrollBar.unitIncrement = 16
        }
        tabPane.addTab("Branches", AllIcons.Vcs.Branch, branchScroll)

        // CardLayout
        listPanel.add(tabPane, BorderLayout.CENTER)
        cardPanel.add(listPanel, LIST)
        cardPanel.add(detailContainer, DETAIL)
        cards.show(cardPanel, LIST)
        add(cardPanel, BorderLayout.CENTER)
    }

    private fun regKeys() {
        registerKeyboardAction({ refresh() }, KeyStroke.getKeyStroke("R"), WHEN_IN_FOCUSED_WINDOW)
        registerKeyboardAction(
            { if (detailContainer.isVisible) cards.show(cardPanel, LIST) },
            KeyStroke.getKeyStroke("ESCAPE"), WHEN_IN_FOCUSED_WINDOW
        )
    }

    // ── Rendering ──────────────────────────────────────────────────────────

    private fun renderIssues() {
        issueListPanel.removeAll()
        val filtered = issueData.filter {
            when (issueFilterState) {
                "open" -> it.state == IssueState.OPEN
                "closed" -> it.state == IssueState.CLOSED
                else -> true
            }
        }

        if (filtered.isEmpty()) {
            issueListPanel.add(JBLabel("No issues found", SwingConstants.CENTER).apply {
                border = JBUI.Borders.empty(20); foreground = UIUtil.getContextHelpForeground()
            })
        } else {
            filtered.forEach { issue -> issueListPanel.add(createIssueRow(issue)) }
        }
        issueListPanel.revalidate(); issueListPanel.repaint()
    }

    private fun createIssueRow(issue: Issue): JPanel {
        val rowHeight = JBUI.scale(54)
        val row = object : JPanel(GridBagLayout()) {
            override fun getMaximumSize(): Dimension = Dimension(super.getMaximumSize().width, rowHeight)
        }.apply {
            background = UIUtil.getTableBackground()
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLineBottom(UIUtil.getBoundsColor()), JBUI.Borders.empty(10, 14)
            )
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            minimumSize = Dimension(0, rowHeight)
            preferredSize = Dimension(preferredSize.width, rowHeight)
        }

        val badge = StateBadge().apply { setIssueState(issue.state) }

        val numberLabel = JBLabel("#${issue.number} ").apply {
            font = JBUI.Fonts.label().asBold()
            foreground = UIUtil.getContextHelpForeground()
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { issue.url.let { BrowserUtil.browse(it) } }
                override fun mouseEntered(e: MouseEvent) { foreground = JBUI.CurrentTheme.Link.Foreground.HOVERED }
                override fun mouseExited(e: MouseEvent) { foreground = UIUtil.getContextHelpForeground() }
            })
        }
        val title = JBLabel(issue.title).apply { font = JBUI.Fonts.label() }
        val meta = JBLabel("by ${issue.author} · ${fmt(issue.updatedAt)}").apply {
            font = SF; foreground = UIUtil.getContextHelpForeground()
        }

        row.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) { row.background = UIUtil.getTableSelectionBackground(false) }
            override fun mouseExited(e: MouseEvent) { row.background = UIUtil.getTableBackground() }
            override fun mouseClicked(e: MouseEvent) { showIssueDetail(issue) }
        })

        val c = GridBagConstraints()
        c.anchor = GridBagConstraints.WEST; c.gridx = 0; c.weightx = 0.0; c.insets = JBUI.insetsRight(8)
        row.add(badge, c)
        c.gridx = 1; c.weightx = 0.0; c.insets = JBUI.insetsRight(0)
        row.add(numberLabel, c)
        c.gridx = 2; c.weightx = 1.0; c.insets = JBUI.insets(0)
        row.add(title, c)
        c.gridx = 0; c.gridy = 1; c.gridwidth = 3; c.weightx = 0.0; c.insets = JBUI.insetsTop(6)
        row.add(meta, c)

        return row
    }

    private fun renderPRs() {
        prListPanel.removeAll()
        val filtered = prData.filter {
            when (prFilterState) {
                "open" -> it.state == PRState.OPEN
                "merged" -> it.state == PRState.MERGED
                "closed" -> it.state == PRState.CLOSED
                else -> true
            }
        }

        if (filtered.isEmpty()) {
            prListPanel.add(JBLabel("No pull requests found", SwingConstants.CENTER).apply {
                border = JBUI.Borders.empty(20); foreground = UIUtil.getContextHelpForeground()
            })
        } else {
            filtered.forEach { pr -> prListPanel.add(createPRRow(pr)) }
        }
        prListPanel.revalidate(); prListPanel.repaint()
    }

    private fun createPRRow(pr: PullRequest): JPanel {
        val rowHeight = JBUI.scale(54)
        val row = object : JPanel(GridBagLayout()) {
            override fun getMaximumSize(): Dimension = Dimension(super.getMaximumSize().width, rowHeight)
        }.apply {
            background = UIUtil.getTableBackground()
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLineBottom(UIUtil.getBoundsColor()), JBUI.Borders.empty(10, 14)
            )
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            minimumSize = Dimension(0, rowHeight)
            preferredSize = Dimension(preferredSize.width, rowHeight)
        }
        val badge = StateBadge().apply { setPRState(pr.state) }

        val numberLabel = JBLabel("#${pr.number} ").apply {
            font = JBUI.Fonts.label().asBold()
            foreground = UIUtil.getContextHelpForeground()
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { BrowserUtil.browse(pr.url) }
                override fun mouseEntered(e: MouseEvent) { foreground = JBUI.CurrentTheme.Link.Foreground.HOVERED }
                override fun mouseExited(e: MouseEvent) { foreground = UIUtil.getContextHelpForeground() }
            })
        }
        val title = JBLabel(pr.title).apply { font = JBUI.Fonts.label() }
        val meta = JBLabel("${pr.sourceBranch} → ${pr.targetBranch} · ${fmt(pr.updatedAt)}").apply {
            font = SF; foreground = UIUtil.getContextHelpForeground()
        }

        row.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) { row.background = UIUtil.getTableSelectionBackground(false) }
            override fun mouseExited(e: MouseEvent) { row.background = UIUtil.getTableBackground() }
            override fun mouseClicked(e: MouseEvent) { showPRDetail(pr) }
        })

        val c = GridBagConstraints()
        c.anchor = GridBagConstraints.WEST; c.gridx = 0; c.weightx = 0.0; c.insets = JBUI.insetsRight(8)
        row.add(badge, c)
        c.gridx = 1; c.weightx = 0.0; c.insets = JBUI.insetsRight(0)
        row.add(numberLabel, c)
        c.gridx = 2; c.weightx = 1.0; c.insets = JBUI.insets(0)
        row.add(title, c)
        c.gridx = 0; c.gridy = 1; c.gridwidth = 3; c.weightx = 0.0; c.insets = JBUI.insetsTop(6)
        row.add(meta, c)
        return row
    }

    private fun renderBranches() {
        branchListPanel.removeAll()
        branchData.forEach { branch -> branchListPanel.add(createBranchRow(branch)) }
        branchListPanel.revalidate(); branchListPanel.repaint()
    }

    private fun createBranchRow(branch: GitBranch): JPanel {
        val rowHeight = JBUI.scale(34)
        val row = object : JPanel(GridBagLayout()) {
            override fun getMaximumSize(): Dimension = Dimension(super.getMaximumSize().width, rowHeight)
        }.apply {
            background = UIUtil.getTableBackground()
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLineBottom(UIUtil.getBoundsColor()), JBUI.Borders.empty(6, 12)
            )
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            minimumSize = Dimension(0, rowHeight)
            preferredSize = Dimension(preferredSize.width, rowHeight)
        }
        val nameLabel = JBLabel(branch.name).apply {
            font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { checkout(branch.name) }
                override fun mouseEntered(e: MouseEvent) { foreground = JBUI.CurrentTheme.Link.Foreground.HOVERED }
                override fun mouseExited(e: MouseEvent) { foreground = UIUtil.getLabelForeground() }
            })
        }
        val sha = JBLabel(branch.sha.take(7)).apply {
            font = Font(Font.MONOSPACED, Font.PLAIN, 10)
            foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            toolTipText = "Open commit in browser"
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val o = remoteOwner ?: return; val r = remoteRepo ?: return
                    BrowserUtil.browse("https://github.com/$o/$r/tree/${branch.name}")
                }
                override fun mouseEntered(e: MouseEvent) { foreground = JBUI.CurrentTheme.Link.Foreground.HOVERED }
                override fun mouseExited(e: MouseEvent) { foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED }
            })
        }

        row.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) { row.background = UIUtil.getTableSelectionBackground(false) }
            override fun mouseExited(e: MouseEvent) { row.background = UIUtil.getTableBackground() }
        })

        val c = GridBagConstraints()
        if (branch.isDefault) {
            c.gridx = 0; c.weightx = 0.0; c.insets = JBUI.insetsRight(6); c.anchor = GridBagConstraints.WEST
            row.add(JLabel(AllIcons.Vcs.Branch), c)
        }
        c.gridx = if (branch.isDefault) 1 else 0; c.weightx = 1.0; c.anchor = GridBagConstraints.WEST
        row.add(nameLabel, c)
        c.gridx = 2; c.weightx = 0.0; c.anchor = GridBagConstraints.EAST; c.insets = JBUI.insetsLeft(8)
        row.add(sha, c)
        return row
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    private fun showIssueDetail(issue: Issue) {
        try {
            val detail = IssueDetailPanel(
                provider, remoteOwner ?: "", remoteRepo ?: "", issue,
                onBack = { cards.show(cardPanel, LIST) },
                onRefresh = { refresh(silent = true) }
            )
            showDetailContent(detail)
        } catch (e: Exception) {
            val msg = "${e.javaClass.simpleName}: ${e.message}"
            e.printStackTrace()
            statusLabel.text = msg
            statusLabel.foreground = UIUtil.getErrorForeground()
            JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun showPRDetail(pr: PullRequest) {
        try {
            val detail = PullRequestDetailPanel(
                provider, remoteOwner ?: "", remoteRepo ?: "", pr,
                onBack = { cards.show(cardPanel, LIST) },
                onRefresh = { refresh(silent = true) }
            )
            showDetailContent(detail)
        } catch (e: Exception) {
            val msg = "${e.javaClass.simpleName}: ${e.message}"
            e.printStackTrace()
            statusLabel.text = msg
            statusLabel.foreground = UIUtil.getErrorForeground()
            JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun showDetailContent(c: JComponent) {
        detailContainer.removeAll()
        detailContainer.add(c, BorderLayout.CENTER)
        cards.show(cardPanel, DETAIL)
    }

    fun reloadConfig() {
        remoteDetected = gitDetector.hasRemote() && provider.isConfigured()
        if (remoteDetected) {
            val info = gitDetector.detect()
            if (info != null) {
                remoteOwner = info.owner; remoteRepo = info.repoName
                repoLabel.text = "${info.owner}/${info.repoName}"
                branchLabel.text = "  ·  ${info.currentBranch ?: "unknown"}"
            }
            cards.show(cardPanel, LIST)
            refresh()
        } else {
            repoLabel.text = if (!gitDetector.hasRemote()) "No remote" else "No token"
            branchLabel.text = ""
            statusLabel.text =
                if (!gitDetector.hasRemote()) "Set a git remote first" else "Configure token in Settings"
            issueData = emptyList(); prData = emptyList(); branchData = emptyList()
            renderIssues(); renderPRs(); renderBranches()
        }
    }

    fun refresh(silent: Boolean = false) {
        if (remoteOwner == null || remoteRepo == null) { reloadConfig(); return }
        statusLabel.text = "Loading..."; statusLabel.foreground = UIUtil.getContextHelpForeground()

        thread {
            try {
                val issues = runBlocking { provider.getIssues(remoteOwner!!, remoteRepo!!, "open", null, null) }
                val closed = runBlocking { provider.getIssues(remoteOwner!!, remoteRepo!!, "closed", null, null) }
                val prsOpen = runBlocking { provider.getPullRequests(remoteOwner!!, remoteRepo!!, "open") }
                val prsClosed = runBlocking { provider.getPullRequests(remoteOwner!!, remoteRepo!!, "closed") }
                val branches = runBlocking { provider.getBranches(remoteOwner!!, remoteRepo!!) }
                SwingUtilities.invokeLater {
                    issueData = issues + closed; renderIssues()
                    prData = prsOpen + prsClosed; renderPRs()
                    branchData = branches; renderBranches()

                    tabPane.setTitleAt(0, "Issues (${issueData.size})")
                    tabPane.setTitleAt(1, "PRs (${prData.size})")
                    tabPane.setTitleAt(2, "Branches (${branches.size})")
                    statusLabel.text = "Synced successfully"
                    statusLabel.foreground = UIUtil.getActiveTextColor()
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    statusLabel.text = "Error syncing data"
                    statusLabel.foreground = UIUtil.getErrorForeground()
                    JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(
                        e.message ?: "Failed to fetch data", MessageType.ERROR, null
                    ).setTitle("Sync Failed").createBalloon().show(
                        JBPopupFactory.getInstance().guessBestPopupLocation(tabPane), Balloon.Position.below
                    )
                }
            }
        }
    }

    private fun checkout(name: String) {
        try {
            val r = gitDetector.detect()?.gitRoot ?: return
            Runtime.getRuntime().exec(arrayOf("git", "checkout", name), null, r)
            JOptionPane.showMessageDialog(null, "Switched to $name")
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, "Failed: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun createIssue() {
        val o = remoteOwner ?: return; val r = remoteRepo ?: return
        CreateIssueDialog(myProject, o, r, provider).showAndGet()
        refresh()
    }

    private fun createPR() {
        val o = remoteOwner ?: return; val r = remoteRepo ?: return
        BrowserUtil.browse("https://github.com/$o/$r/compare")
    }
}

// ── Enhanced Create Issue Dialog ────────────────────────────────────────────

class CreateIssueDialog(
    project: Project?,
    private val owner: String,
    private val repo: String,
    private val provider: GitHubProvider
) : com.intellij.openapi.ui.DialogWrapper(project) {

    private val tf = JBTextField().apply { emptyText.text = "Issue title" }
    private val ta = JBTextArea().apply {
        emptyText.text = "Leave a description..."; lineWrap = true; rows = 6
        font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.Fonts.label().size)
    }
    private val assigneeField = JBTextField().apply {
        emptyText.text = "e.g. username (comma-separated)"
    }

    // Label selection
    private val labelChipsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2)).apply { isOpaque = false }
    private val selectedLabels = mutableSetOf<VsLabel>()
    private var repoLabels: List<VsLabel> = emptyList()

    init {
        title = "New Issue — $owner/$repo"
        init()
        fetchLabels()
    }

    private fun fetchLabels() {
        thread {
            try {
                repoLabels = runBlocking { provider.getLabels(owner, repo) }
                SwingUtilities.invokeLater { renderLabelChips() }
            } catch (_: Exception) {
                SwingUtilities.invokeLater {
                    labelChipsPanel.add(JBLabel(" (failed to load labels)").apply {
                        font = RemoteVcsToolWindowPanel.SF
                        foreground = com.intellij.util.ui.UIUtil.getContextHelpForeground()
                    })
                    labelChipsPanel.revalidate()
                }
            }
        }
    }

    private fun renderLabelChips() {
        labelChipsPanel.removeAll()
        repoLabels.forEach { label ->
            val chip = JBLabel(" ${label.name} ").apply {
                font = RemoteVcsToolWindowPanel.SF
                isOpaque = true
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                border = JBUI.Borders.empty(2, 6)
                // Use label color for background
                val bgColor = try {
                    Color(
                        label.color.substring(0, 2).toInt(16),
                        label.color.substring(2, 4).toInt(16),
                        label.color.substring(4, 6).toInt(16)
                    )
                } catch (_: Exception) { Color.GRAY }
                val isDark = (bgColor.red * 299 + bgColor.green * 587 + bgColor.blue * 114) / 1000 < 140
                foreground = if (isDark) Color.WHITE else Color.BLACK
                background = bgColor

                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        if (selectedLabels.contains(label)) {
                            selectedLabels.remove(label)
                            border = JBUI.Borders.empty(2, 6)
                        } else {
                            selectedLabels.add(label)
                            border = BorderFactory.createCompoundBorder(
                                JBUI.Borders.customLine(com.intellij.util.ui.UIUtil.getLabelForeground(), 2),
                                JBUI.Borders.empty(2, 6)
                            )
                        }
                    }
                })
            }
            labelChipsPanel.add(chip)
        }
        labelChipsPanel.revalidate(); labelChipsPanel.repaint()
    }

    override fun createCenterPanel(): JComponent {
        val form = com.intellij.util.ui.FormBuilder.createFormBuilder()
            .addLabeledComponent("Title:", tf, true)
            .addVerticalGap(8)
            .addLabeledComponent("Description:", JBScrollPane(ta), true)
            .addVerticalGap(8)
            .addLabeledComponent("Assignees:", assigneeField, true)
            .addVerticalGap(8)
            .addLabeledComponent("Labels:", JPanel(BorderLayout()).apply {
                isOpaque = false
                add(labelChipsPanel, BorderLayout.CENTER)
            }, true)

        return JPanel(BorderLayout()).apply {
            add(form.panel.apply {
                border = JBUI.Borders.empty(8)
                preferredSize = Dimension(520, 400)
            }, BorderLayout.CENTER)
        }
    }

    override fun doOKAction() {
        val t = tf.text.trim()
        if (t.isBlank()) {
            com.intellij.openapi.ui.Messages.showErrorDialog(
                "Title is required to create an issue.", "Validation Error"
            )
            return
        }

        val issueLabels: List<String>? = if (selectedLabels.isEmpty()) null else selectedLabels.map { it.name }
        val issueAssignees = assigneeField.text.trim()
            .takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }

        ProgressManager.getInstance().run(object :
            com.intellij.openapi.progress.Task.Backgroundable(null, "Creating issue...", true) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                indicator.isIndeterminate = true
                try {
                    runBlocking {
                        provider.createIssue(owner, repo, t, ta.text.ifBlank { null }, issueLabels, issueAssignees)
                    }
                    ApplicationManager.getApplication().invokeLater {
                        this@CreateIssueDialog.close(OK_EXIT_CODE)
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        com.intellij.openapi.ui.Messages.showErrorDialog(
                            "Failed: ${e.message}", "Error"
                        )
                    }
                }
            }
        })
    }
}
