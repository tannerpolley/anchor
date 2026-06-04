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
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.itsjeel01.remotevcsmanager.GitRemoteDetector
import com.itsjeel01.remotevcsmanager.models.*
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.settings.RemoteVcsSettingsState
import com.itsjeel01.remotevcsmanager.settings.SettingsChangeNotifier
import com.itsjeel01.remotevcsmanager.ui.components.StateBadge
import com.itsjeel01.remotevcsmanager.ui.detail.IssueDetailPanel
import kotlinx.coroutines.runBlocking
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import kotlin.concurrent.thread

class RemoteVcsToolWindowPanel(project: Project) : SimpleToolWindowPanel(true, true) {

    private val gitDetector = GitRemoteDetector(project)
    private val provider = GitHubProvider()
    private val myProject = project

    private var remoteOwner: String? = null; private var remoteRepo: String? = null; private var remoteDetected = false

    // Header — modern, clean
    private val statusDot = JLabel().apply { foreground = JBColor(0x2DA44E, 0x3FB950); font = font.deriveFont(16f) }
    private val repoLabel = JBLabel("No remote").apply { font = JBUI.Fonts.label(12f).asBold() }
    private val branchLabel = JBLabel().apply { font = JBUI.Fonts.smallFont(); foreground = UIUtil.getContextHelpForeground() }
    private val statusLabel = JBLabel("Ready").apply { font = JBUI.Fonts.smallFont(); foreground = UIUtil.getContextHelpForeground() }

    // Card layout
    private val cardPanel = JPanel(CardLayout())
    private val listPanel = JPanel(BorderLayout())
    private val detailContainer = JPanel(BorderLayout())
    private val emptyDetail = JBLabel("Select an item", SwingConstants.CENTER).apply { foreground = UIUtil.getContextHelpForeground() }
    private val cards: CardLayout get() = cardPanel.layout as CardLayout

    // Filters
    private val issueFilter = JComboBox(arrayOf("All", "Open", "Closed"))
    private val prFilter = JComboBox(arrayOf("Open", "Merged", "Closed", "All"))

    // Tabs + lists
    private val tabPane = JTabbedPane().apply { font = JBUI.Fonts.label() }
    private val issueModel = DefaultListModel<Issue?>()
    private val prModel = DefaultListModel<PullRequest?>()
    private val branchModel = DefaultListModel<GitBranch?>()
    private val issueList = JBList<Issue?>().apply { cellRenderer = IssueCardRenderer(); selectionMode = ListSelectionModel.SINGLE_SELECTION }
    private val prList = JBList<PullRequest?>().apply { cellRenderer = PRCardRenderer(); selectionMode = ListSelectionModel.SINGLE_SELECTION }
    private val branchList = JBList<GitBranch?>().apply { cellRenderer = BranchCardRenderer(); selectionMode = ListSelectionModel.SINGLE_SELECTION }

    // Data
    private var issueData: List<Issue> = emptyList(); private var prData: List<PullRequest> = emptyList()

    init {
        buildUI(); regKeys(); reloadConfig()
        // React to token changes without restart
        val connection = ApplicationManager.getApplication().messageBus.connect()
        connection.subscribe(SettingsChangeNotifier.SETTINGS_CHANGED,
            SettingsChangeNotifier.SettingsChangeListener { reloadConfig() })
    }

    companion object {
        const val LIST = "list"; const val DETAIL = "detail"
        fun fmt(iso: String): String = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val d = sdf.parse(iso.take(19)) ?: return iso
            val diff = Date().time - d.time; val m = diff / 60000; val h = m / 60; val dd = h / 24; val w = dd / 7
            when { m < 1 -> "now"; m < 60 -> "${m}m"; h < 24 -> "${h}h"; dd < 7 -> "${dd}d"; w < 5 -> "${w}w"; else -> SimpleDateFormat("MMM d", Locale.US).format(d) }
        } catch (_: Exception) { iso }
        val SF = JBUI.Fonts.smallFont(); val CP = BorderFactory.createEmptyBorder(5, 8, 5, 8)
    }

    private fun regKeys() {
        registerKeyboardAction({ refresh() }, KeyStroke.getKeyStroke("R"), WHEN_IN_FOCUSED_WINDOW)
        registerKeyboardAction({ createIssue() }, KeyStroke.getKeyStroke("N"), WHEN_IN_FOCUSED_WINDOW)
        registerKeyboardAction({ openDetail() }, KeyStroke.getKeyStroke("ENTER"), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        registerKeyboardAction({ if (detailContainer.isVisible) cards.show(cardPanel, LIST) }, KeyStroke.getKeyStroke("ESCAPE"), WHEN_IN_FOCUSED_WINDOW)
        registerKeyboardAction({ if (tabPane.selectedIndex == 0) issueFilter.requestFocusInWindow() else prFilter.requestFocusInWindow() }, KeyStroke.getKeyStroke("/"), WHEN_IN_FOCUSED_WINDOW)
    }

    private fun buildUI() {
        // Toolbar
        val tg = DefaultActionGroup().apply {
            add(object : AnAction("Refresh (R)", "Reload", AllIcons.Actions.Refresh) { override fun actionPerformed(e: AnActionEvent) = refresh(); override fun update(e: AnActionEvent) { e.presentation.isEnabled = true } })
            addSeparator()
            add(object : AnAction("New Issue (N)", "Create", AllIcons.General.Add) { override fun actionPerformed(e: AnActionEvent) = createIssue(); override fun update(e: AnActionEvent) { e.presentation.isEnabled = remoteDetected && provider.isConfigured() } })
            addSeparator()
            add(object : AnAction("Open Repo", "", AllIcons.Ide.External_link_arrow) { override fun actionPerformed(e: AnActionEvent) { val o = remoteOwner ?: return; val r = remoteRepo ?: return; BrowserUtil.browse("https://github.com/$o/$r") }; override fun update(e: AnActionEvent) { e.presentation.isEnabled = remoteDetected } })
            addSeparator()
            add(object : AnAction("Settings", "", AllIcons.General.Settings) { override fun actionPerformed(e: AnActionEvent) { com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(myProject, "Remote VCS Manager") } })
        }
        val tb = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, tg, true); tb.targetComponent = this
        add(JPanel(BorderLayout()).apply { add(tb.component, BorderLayout.WEST); add(statusLabel, BorderLayout.CENTER); border = JBUI.Borders.empty(1, 4) }, BorderLayout.NORTH)

        // Header — moderna
        add(JPanel(BorderLayout()).apply {
            background = UIUtil.getTableBackground(); border = JBUI.Borders.empty(4, 8)
            add(JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply { isOpaque = false
                add(JLabel(AllIcons.Vcs.Branch).apply { border = JBUI.Borders.emptyRight(4) })
                add(repoLabel); add(branchLabel)
            }, BorderLayout.WEST)
        }, BorderLayout.BEFORE_FIRST_LINE)

        // Issue tab
        issueFilter.addActionListener { filterIssues() }
        issueList.model = issueModel; issueList.addListSelectionListener { if (!it.valueIsAdjusting) openDetail() }
        tabPane.addTab("Issues", AllIcons.General.TodoDefault, JPanel(BorderLayout()).apply {
            add(JPanel(FlowLayout(FlowLayout.LEFT, 4, 2)).apply { isOpaque = false; add(JBLabel("State:").apply { font = SF }); add(issueFilter) }, BorderLayout.NORTH)
            add(JBScrollPane(issueList).apply { border = JBUI.Borders.empty() }, BorderLayout.CENTER)
        })

        // PR tab
        prFilter.addActionListener { filterPRs() }
        prList.model = prModel; prList.addListSelectionListener { if (!it.valueIsAdjusting) openDetail() }
        tabPane.addTab("PRs", AllIcons.Vcs.Merge, JPanel(BorderLayout()).apply {
            add(JPanel(FlowLayout(FlowLayout.LEFT, 4, 2)).apply { isOpaque = false; add(JBLabel("State:").apply { font = SF }); add(prFilter) }, BorderLayout.NORTH)
            add(JBScrollPane(prList).apply { border = JBUI.Borders.empty() }, BorderLayout.CENTER)
        })

        // Branch tab
        branchList.model = branchModel; branchList.addListSelectionListener { if (!it.valueIsAdjusting) openDetail() }
        tabPane.addTab("Branches", AllIcons.Vcs.Branch, JBScrollPane(branchList).apply { border = JBUI.Borders.empty() })

        // Card layout
        listPanel.add(tabPane, BorderLayout.CENTER); detailContainer.add(emptyDetail, BorderLayout.CENTER)
        cardPanel.add(listPanel, LIST); cardPanel.add(detailContainer, DETAIL)
        cards.show(cardPanel, LIST); add(cardPanel, BorderLayout.CENTER)
    }

    // ── Navigation ──────────────────────────────────────────────
    private fun openDetail() {
        when (tabPane.selectedIndex) {
            0 -> issueList.selectedValue?.let { showDetail(buildIssueDetail(it)) }
            1 -> prList.selectedValue?.let { showDetail(buildPRDetail(it)) }
            2 -> branchList.selectedValue?.let { showDetail(buildBranchDetail(it)) }
        }
    }

    private fun showDetail(c: JComponent) {
        detailContainer.removeAll()
        // Minimal back link (not a button)
        val back = JBLabel("  \u2190  Back").apply {
            font = SF; foreground = JBUI.CurrentTheme.Link.linkColor()
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.empty(4, 8)
            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) { cards.show(cardPanel, LIST) }
                override fun mouseEntered(e: java.awt.event.MouseEvent) { background = UIUtil.getTableSelectionBackground(false); isOpaque = true }
                override fun mouseExited(e: java.awt.event.MouseEvent) { isOpaque = false }
            })
        }
        detailContainer.add(JPanel(BorderLayout()).apply { background = UIUtil.getTableBackground(); border = JBUI.Borders.empty(0, 0, 1, 0); add(back, BorderLayout.WEST) }, BorderLayout.NORTH)
        detailContainer.add(c, BorderLayout.CENTER)
        cards.show(cardPanel, DETAIL)
    }

    private fun buildIssueDetail(i: Issue) = IssueDetailPanel(provider, remoteOwner ?: "", remoteRepo ?: "", i, onBack = { cards.show(cardPanel, LIST) }, onRefresh = { refresh() })

    private fun buildPRDetail(p: PullRequest): JComponent {
        val panel = JPanel(BorderLayout()).apply { border = JBUI.Borders.empty(8, 10) }
        panel.add(JPanel(BorderLayout()).apply { isOpaque = false; add(StateBadge().apply { setPRState(p.state) }, BorderLayout.WEST); add(JBLabel("  #${p.id} ${p.title}").apply { font = JBUI.Fonts.label(13f).asBold() }, BorderLayout.CENTER) }, BorderLayout.NORTH)
        panel.add(JBLabel("${p.sourceBranch} \u2192 ${p.targetBranch} \u00B7 ${fmt(p.updatedAt)}").apply { font = SF; foreground = UIUtil.getContextHelpForeground() }, BorderLayout.CENTER)
        panel.add(flatIconBtn(AllIcons.Ide.External_link_arrow, "Open in browser") { BrowserUtil.browse(p.url) }, BorderLayout.SOUTH)
        return panel
    }

    private fun buildBranchDetail(b: GitBranch): JComponent {
        val panel = JPanel(BorderLayout()).apply { border = JBUI.Borders.empty(8, 10) }
        panel.add(JBLabel(b.name).apply { font = JBUI.Fonts.label(13f).asBold() }, BorderLayout.NORTH)
        val acts = JPanel(FlowLayout(FlowLayout.LEFT, 4, 4)).apply { isOpaque = false
            add(JButton("Checkout", AllIcons.Vcs.Branch).apply { addActionListener { checkout(b.name) } })
            add(JButton(AllIcons.Ide.External_link_arrow).apply { addActionListener { BrowserUtil.browse("https://github.com/${remoteOwner}/${remoteRepo}/tree/${b.name}") } })
        }
        panel.add(acts, BorderLayout.SOUTH)
        return panel
    }

    // ── Filters ────────────────────────────────────────────────
    private fun filterIssues() {
        val f = issueFilter.selectedItem as? String ?: "All"
        issueModel.clear(); issueData.filter { when (f) { "Open" -> it.state == IssueState.OPEN; "Closed" -> it.state == IssueState.CLOSED; else -> true } }.forEach { issueModel.addElement(it) }
    }
    private fun filterPRs() {
        val f = prFilter.selectedItem as? String ?: "Open"
        prModel.clear(); prData.filter { when (f) { "Open" -> it.state == PRState.OPEN; "Merged" -> it.state == PRState.MERGED; "Closed" -> it.state == PRState.CLOSED; else -> true } }.forEach { prModel.addElement(it) }
    }

    // ── Data ───────────────────────────────────────────────────
    fun reloadConfig() {
        remoteDetected = gitDetector.hasRemote() && provider.isConfigured()
        if (remoteDetected) {
            val info = gitDetector.detect()
            if (info != null) { remoteOwner = info.owner; remoteRepo = info.repoName; repoLabel.text = "${info.owner}/${info.repoName}"; branchLabel.text = "  \u00B7  ${info.currentBranch ?: "unknown"}" }
            refresh()
        } else {
            repoLabel.text = if (!gitDetector.hasRemote()) "No remote" else "No token"; branchLabel.text = ""
            statusLabel.text = if (!gitDetector.hasRemote()) "Set a git remote first" else "Configure token in Settings \u2699"; clearAll()
        }
    }

    fun refresh() {
        if (remoteOwner == null || remoteRepo == null) { reloadConfig(); return }
        cards.show(cardPanel, LIST); statusLabel.text = "Loading..."; statusLabel.foreground = UIUtil.getContextHelpForeground()
        skeleton()
        thread {
            try {
                val issues = runBlocking { provider.getIssues(remoteOwner!!, remoteRepo!!, "open", null, null) }
                val closed = runBlocking { provider.getIssues(remoteOwner!!, remoteRepo!!, "closed", null, null) }
                val prs = runBlocking { provider.getPullRequests(remoteOwner!!, remoteRepo!!, "open") }
                val branches = runBlocking { provider.getBranches(remoteOwner!!, remoteRepo!!) }
                SwingUtilities.invokeLater {
                    issueData = issues + closed; filterIssues(); prData = prs; filterPRs()
                    branchModel.clear(); branches.forEach { branchModel.addElement(it) }
                    tabPane.setTitleAt(0, "Issues (${issues.size})")
                    statusLabel.text = "${issues.size} open, ${prs.size} PRs, ${branches.size} branches"; statusLabel.foreground = UIUtil.getActiveTextColor()
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater { statusLabel.text = "Error: ${e.message}"; statusLabel.foreground = UIUtil.getErrorForeground()
                    JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(e.message ?: "", MessageType.ERROR, null).setTitle("Failed").createBalloon().show(JBPopupFactory.getInstance().guessBestPopupLocation(tabPane), Balloon.Position.below) }
            }
        }
    }

    private fun skeleton() { issueModel.clear(); prModel.clear(); branchModel.clear(); repeat(3) { issueModel.addElement(null); prModel.addElement(null); branchModel.addElement(null) } }
    private fun clearAll() { issueModel.clear(); prModel.clear(); branchModel.clear(); issueData = emptyList(); prData = emptyList() }

    private fun checkout(name: String) {
        try { val r = gitDetector.detect()?.gitRoot ?: return; Runtime.getRuntime().exec(arrayOf("git", "checkout", name), null, r); JOptionPane.showMessageDialog(null, "Switched to $name") }
        catch (e: Exception) { JOptionPane.showMessageDialog(null, "Failed: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE) }
    }

    private fun createIssue() { val o = remoteOwner ?: return; val r = remoteRepo ?: return; CreateIssueDialog(myProject, o, r, provider); refresh() }
    private fun flatIconBtn(icon: javax.swing.Icon, tip: String, action: () -> Unit) = javax.swing.JButton(icon).apply { isOpaque = false; border = JBUI.Borders.empty(2, 4); isBorderPainted = false; toolTipText = tip; addActionListener { action() } }

    // ── Renderers ──────────────────────────────────────────────

    private open class IssueCardRenderer : JPanel(BorderLayout()), ListCellRenderer<Issue?> {
        private val title = JLabel().apply { font = JBUI.Fonts.label() }
        private val meta = JLabel().apply { font = SF; foreground = UIUtil.getContextHelpForeground() }
        private val badge = StateBadge()
        private val actions = JPanel(FlowLayout(FlowLayout.RIGHT, 2, 0)).apply { isOpaque = false }
        private val openBtn = JButton(AllIcons.Ide.External_link_arrow); private val copyBtn = JButton(AllIcons.Actions.Copy)
        private var url: String? = null
        init {
            openBtn.apply { isOpaque = false; border = JBUI.Borders.empty(1, 3); isBorderPainted = false; isVisible = false; addActionListener { url?.let { BrowserUtil.browse(it) } } }
            copyBtn.apply { isOpaque = false; border = JBUI.Borders.empty(1, 3); isBorderPainted = false; isVisible = false; addActionListener { url?.let { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(it), null) } } }
            actions.add(openBtn); actions.add(copyBtn)
        }
        override fun getListCellRendererComponent(list: JList<out Issue?>, i: Issue?, idx: Int, sel: Boolean, foc: Boolean): Component {
            removeAll()
            if (i == null) { add(JPanel().apply { background = UIUtil.getTableBackground(); border = CP; preferredSize = Dimension(0, 34) }); return this }
            url = i.url; title.text = "#${i.number} ${i.title}"; if (i.title.length > 80) title.text = "#${i.number} ${i.title.take(77)}..."
            title.foreground = if (sel) UIUtil.getTableSelectionForeground(true) else UIUtil.getLabelForeground()
            meta.text = "by ${i.author} \u00B7 ${fmt(i.updatedAt)}"; badge.setIssueState(i.state); openBtn.isVisible = sel; copyBtn.isVisible = sel
            val g = JPanel(GridBagLayout()).apply { isOpaque = false }; val c = GridBagConstraints()
            c.anchor = GridBagConstraints.WEST; c.gridx = 0; c.weightx = 0.0; c.insets = JBUI.insetsRight(6); g.add(badge, c)
            c.gridx = 1; c.weightx = 1.0; c.insets = JBUI.insets(0); g.add(title, c)
            c.gridx = 2; c.weightx = 0.0; c.insets = JBUI.insets(0); g.add(actions, c)
            c.gridx = 0; c.gridy = 1; c.gridwidth = 3; c.weightx = 0.0; c.insets = JBUI.insetsTop(1); g.add(meta, c)
            add(g, BorderLayout.CENTER); background = if (sel) UIUtil.getTableSelectionBackground(true) else UIUtil.getTableBackground(); border = CP; return this
        }
    }

    private open class PRCardRenderer : JPanel(BorderLayout()), ListCellRenderer<PullRequest?> {
        private val title = JLabel().apply { font = JBUI.Fonts.label() }; private val meta = JLabel().apply { font = SF; foreground = UIUtil.getContextHelpForeground() }; private val badge = StateBadge()
        override fun getListCellRendererComponent(list: JList<out PullRequest?>, p: PullRequest?, idx: Int, sel: Boolean, foc: Boolean): Component {
            removeAll()
            if (p == null) { add(JPanel().apply { background = UIUtil.getTableBackground(); border = CP; preferredSize = Dimension(0, 34) }); return this }
            title.text = if (p.title.length > 80) "#${p.id} ${p.title.take(77)}..." else "#${p.id} ${p.title}"; title.foreground = if (sel) UIUtil.getTableSelectionForeground(true) else UIUtil.getLabelForeground()
            meta.text = "${p.sourceBranch} \u2192 ${p.targetBranch} \u00B7 ${fmt(p.updatedAt)}"; badge.setPRState(p.state)
            val g = JPanel(GridBagLayout()).apply { isOpaque = false }; val c = GridBagConstraints()
            c.anchor = GridBagConstraints.WEST; c.gridx = 0; c.weightx = 0.0; c.insets = JBUI.insetsRight(6); g.add(badge, c)
            c.gridx = 1; c.weightx = 1.0; c.insets = JBUI.insets(0); g.add(title, c)
            c.gridx = 0; c.gridy = 1; c.gridwidth = 2; c.weightx = 0.0; c.insets = JBUI.insetsTop(1); g.add(meta, c)
            add(g, BorderLayout.CENTER); background = if (sel) UIUtil.getTableSelectionBackground(true) else UIUtil.getTableBackground(); border = CP; return this
        }
    }

    private open class BranchCardRenderer : JPanel(GridBagLayout()), ListCellRenderer<GitBranch?> {
        private val name = JLabel().apply { font = Font(Font.MONOSPACED, Font.PLAIN, 11) }; private val sha = JLabel().apply { font = Font(Font.MONOSPACED, Font.PLAIN, 10); foreground = UIUtil.getContextHelpForeground() }
        override fun getListCellRendererComponent(list: JList<out GitBranch?>, b: GitBranch?, idx: Int, sel: Boolean, foc: Boolean): Component {
            removeAll()
            if (b == null) { add(JPanel().apply { background = UIUtil.getTableBackground(); border = CP; preferredSize = Dimension(0, 30) }); return this }
            name.text = b.name; name.foreground = if (sel) UIUtil.getTableSelectionForeground(true) else UIUtil.getLabelForeground(); sha.text = b.sha.take(7)
            val c = GridBagConstraints()
            if (b.isDefault) { c.gridx = 0; c.weightx = 0.0; c.insets = JBUI.insetsRight(4); c.anchor = GridBagConstraints.WEST; add(JLabel(AllIcons.Vcs.Branch), c) }
            c.gridx = if (b.isDefault) 1 else 0; c.weightx = 1.0; c.anchor = GridBagConstraints.WEST; c.insets = JBUI.insets(0, if (b.isDefault) 0 else 4, 0, 4)
            add(name, c)
            c.gridx = 2; c.weightx = 0.0; c.anchor = GridBagConstraints.EAST; c.insets = JBUI.insets(0)
            add(sha, c)
            background = if (sel) UIUtil.getTableSelectionBackground(true) else UIUtil.getTableBackground(); border = CP; return this
        }
    }
}

class CreateIssueDialog(project: Project?, private val owner: String, private val repo: String, private val provider: GitHubProvider) : com.intellij.openapi.ui.DialogWrapper(project) {
    private val tf = JBTextField().apply { emptyText.text = "Issue title" }; private val ta = JBTextArea().apply { emptyText.text = "Describe..."; lineWrap = true; rows = 6 }
    init { title = "New Issue — $owner/$repo"; init() }
    override fun createCenterPanel(): JComponent = JPanel(BorderLayout()).apply {
        add(com.intellij.util.ui.FormBuilder.createFormBuilder().addLabeledComponent("Title:", tf, true).addVerticalGap(4).addLabeledComponent("Description:", JBScrollPane(ta), true).panel.apply { border = JBUI.Borders.empty(8, 10); preferredSize = Dimension(480, 280) }, BorderLayout.CENTER)
    }
    override fun doOKAction() {
        val t = tf.text.trim()
        if (t.isBlank()) { com.intellij.openapi.ui.Messages.showErrorDialog("Title required.", "Error"); return }
        ProgressManager.getInstance().run(object : com.intellij.openapi.progress.Task.Backgroundable(null, "Creating...", true) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) { indicator.isIndeterminate = true; try { runBlocking { provider.createIssue(owner, repo, t, ta.text.ifBlank { null }) }; ApplicationManager.getApplication().invokeLater { this@CreateIssueDialog.doOKAction() } } catch (e: Exception) { ApplicationManager.getApplication().invokeLater { com.intellij.openapi.ui.Messages.showErrorDialog("Failed: ${e.message}", "Error") } } }
        })
    }
}
