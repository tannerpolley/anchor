package com.itsjeel01.remotevcsmanager.ui.detail

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.itsjeel01.remotevcsmanager.models.CommitSummary
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.models.PRState
import com.itsjeel01.remotevcsmanager.models.PullRequest
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.components.StateBadge
import kotlinx.coroutines.runBlocking
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import kotlin.concurrent.thread

class PullRequestDetailPanel(
    private val provider: GitHubProvider,
    private val owner: String,
    private val repo: String,
    private val pr: PullRequest,
    private val onBack: () -> Unit,
    private val onRefresh: () -> Unit
) : JPanel(BorderLayout()) {

    private var commits: List<CommitSummary> = emptyList()
    private var comments: List<IssueComment> = emptyList()
    private val sf = JBUI.Fonts.smallFont()
    private val uiFontSize = JBUI.Fonts.label().size

    private val commitsPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS); isOpaque = false }
    private val commitsWrapper = JPanel(BorderLayout()).apply { isOpaque = false; add(commitsPanel, BorderLayout.NORTH) }
    private val commentsPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS); isOpaque = false }
    private val commitsLoadingLabel = JBLabel("Loading commits...").apply {
        font = sf; foreground = UIUtil.getContextHelpForeground()
        border = JBUI.Borders.empty(8, 0); alignmentX = Component.LEFT_ALIGNMENT
    }
    private val commentTA = JBTextArea().apply {
        emptyText.text = "Leave a comment..."; lineWrap = true; wrapStyleWord = true; rows = 3
        font = JBUI.Fonts.label()
        border = BorderFactory.createCompoundBorder(JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1), JBUI.Borders.empty(8))
    }
    private val convScroll = JBScrollPane().apply { border = JBUI.Borders.empty(); verticalScrollBar.unitIncrement = 16 }
    private val commitScroll = JBScrollPane().apply { border = JBUI.Borders.empty(); verticalScrollBar.unitIncrement = 16 }

    init { background = UIUtil.getTableBackground(); buildUI(); loadCommits() }

    private val cardBg: Color get() {
        val base = UIUtil.getTableBackground()
        val r = base.red; val g = base.green; val b = base.blue
        return if (JBColor.isBright()) Color(r.coerceAtMost(248), g.coerceAtMost(248), b.coerceAtMost(248))
        else Color(r.coerceAtLeast(20), g.coerceAtLeast(22), b.coerceAtLeast(28))
    }

    private fun cardPanel(body: JComponent): JPanel {
        val card = JPanel(BorderLayout()).apply {
            background = cardBg
            border = BorderFactory.createCompoundBorder(JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1), JBUI.Borders.empty(8))
            add(body, BorderLayout.CENTER)
        }
        card.alignmentX = Component.LEFT_ALIGNMENT; return card
    }

    private fun buildUI() {
        // Two-line header
        val header = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS); isOpaque = false
            border = BorderFactory.createCompoundBorder(JBUI.Borders.customLineBottom(UIUtil.getBoundsColor()), JBUI.Borders.empty(4, 10))
        }
        val row0 = JPanel(GridBagLayout()).apply { isOpaque = false; alignmentX = 0.0f }
        val backIcon = JLabel(AllIcons.Actions.Back).apply {
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR); toolTipText = "Back"
            border = JBUI.Borders.emptyRight(6)
            addMouseListener(object : MouseAdapter() { override fun mouseClicked(e: MouseEvent) { onBack() } })
        }

        val closeText = when { pr.state == PRState.OPEN -> "Close"; pr.state == PRState.CLOSED -> "Reopen"; else -> "" }
        val closeBg = when { pr.state == PRState.OPEN -> JBColor(0xCF222E, 0xF85149); pr.state == PRState.CLOSED -> JBColor(0x2DA44E, 0x3FB950); else -> Color.DARK_GRAY }
        val closeBtn = JPanel(GridBagLayout()).apply {
            isOpaque = true; background = closeBg; cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.empty(3, 10); isVisible = pr.state != PRState.MERGED
            add(JBLabel(closeText).apply { font = sf; foreground = Color.WHITE })
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { if (pr.state == PRState.OPEN) closePR() else reopenPR() }
                override fun mouseEntered(e: MouseEvent) { background = closeBg.darker() }
                override fun mouseExited(e: MouseEvent) { background = closeBg }
            })
        }
        val shareBtn = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false; cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            toolTipText = "Open in Browser"; border = JBUI.Borders.empty(3, 4)
            add(JBLabel(AllIcons.Ide.External_link_arrow))
            addMouseListener(object : MouseAdapter() { override fun mouseClicked(e: MouseEvent) { BrowserUtil.browse(pr.url) } })
        }

        val gc = GridBagConstraints()
        gc.fill = GridBagConstraints.NONE; gc.anchor = GridBagConstraints.WEST; gc.insets = JBUI.emptyInsets()
        gc.gridx = 0; gc.weightx = 0.0; row0.add(backIcon, gc)
        gc.gridx = 1; gc.weightx = 1.0; gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = JBUI.insetsRight(8)
        row0.add(JBLabel("#${pr.number} ${pr.title}").apply { font = JBUI.Fonts.label(15f).asBold() }, gc)
        gc.gridx = 2; gc.weightx = 0.0; gc.insets = JBUI.insetsRight(4); row0.add(shareBtn, gc)
        gc.gridx = 3; gc.weightx = 0.0; row0.add(closeBtn, gc)
        header.add(row0)

        val metaRow = JPanel(FlowLayout(FlowLayout.LEFT, 3, 0)).apply {
            isOpaque = false; alignmentX = 0.0f
            add(StateBadge().apply { setPRState(pr.state) })
            add(JBLabel(pr.author).apply { font = sf; foreground = UIUtil.getContextHelpForeground() })
            add(JBLabel(fmt(pr.createdAt)).apply { font = sf; foreground = UIUtil.getContextHelpForeground() })
            add(JBLabel("  -  ").apply { font = sf; foreground = UIUtil.getContextHelpForeground() })
            add(makeBranchLink(pr.sourceBranch))
            add(JBLabel("  >  ").apply { font = sf; foreground = UIUtil.getContextHelpForeground() })
            add(makeBranchLink(pr.targetBranch))
        }
        header.add(metaRow)
        add(header, BorderLayout.NORTH)

        // Content wrapper with tabs
        val contentTabs = JTabbedPane().apply { font = JBUI.Fonts.label() }

        // Conversation tab - weighty/ BOTH to top-align
        val convBodyWrapper = JPanel(GridBagLayout()).apply { isOpaque = false }
        val convBody = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS); isOpaque = false; border = JBUI.Borders.empty(4, 0)
        }
        convBodyWrapper.add(convBody, GridBagConstraints().apply {
            gridx = 0; gridy = 0; weightx = 1.0; weighty = 1.0
            fill = GridBagConstraints.BOTH; anchor = GridBagConstraints.NORTH
            insets = JBUI.emptyInsets()
        })

        val rendered = MarkdownRenderer.render(pr.description ?: "_No description provided._")
        val descPane = JEditorPane().apply {
            contentType = "text/html"; isEditable = false; background = cardBg
            font = JBUI.Fonts.label(); putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            text = "<html><body style='${MarkdownRenderer.pageCss(uiFontSize)}'>$rendered</body></html>"
            alignmentX = Component.LEFT_ALIGNMENT
            addHyperlinkListener { event -> if (event.eventType == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) { BrowserUtil.browse(event.url.toString()) } }
        }
        SwingUtilities.invokeLater {
            val pw = descPane.parent?.width ?: return@invokeLater
            if (descPane.width != pw) {
                descPane.setSize(pw, Int.MAX_VALUE)
                val pref = descPane.preferredSize
                descPane.minimumSize = Dimension(pw, pref.height)
                descPane.revalidate()
            }
        }
        convBody.add(cardPanel(descPane))
        convBody.add(Box.createVerticalStrut(8))
        convBody.add(commentsPanel)
        convBody.add(Box.createVerticalGlue())

        convScroll.setViewportView(convBodyWrapper)
        convScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
        SwingUtilities.invokeLater { convScroll.viewport.viewPosition = Point(0, 0) }

        val convTab = JPanel(BorderLayout()).apply { isOpaque = false; border = JBUI.Borders.empty(4, 12) }
        convTab.add(convScroll, BorderLayout.CENTER)

        val postBtn = JButton("Post Comment").apply { addActionListener { postComment() } }
        convTab.add(JPanel(BorderLayout()).apply {
            isOpaque = false; border = JBUI.Borders.emptyTop(8)
            add(commentTA, BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply { isOpaque = false; border = JBUI.Borders.emptyTop(6); add(postBtn) }, BorderLayout.SOUTH)
        }, BorderLayout.SOUTH)
        contentTabs.addTab("Conversation", convTab)

        // Commits tab
        val commitsTab = JPanel(BorderLayout()).apply { isOpaque = false; border = JBUI.Borders.empty(4, 12) }
        commitsPanel.add(commitsLoadingLabel)
        commitScroll.setViewportView(commitsWrapper)
        commitScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
        SwingUtilities.invokeLater { commitScroll.viewport.viewPosition = Point(0, 0) }
        commitsTab.add(commitScroll, BorderLayout.CENTER)
        contentTabs.addTab("Commits", commitsTab)
        add(contentTabs, BorderLayout.CENTER)
    }

    private fun makeBranchLink(name: String): JBLabel {
        val url = "https://github.com/$owner/$repo/tree/$name"
        return JBLabel(name).apply {
            font = sf; foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR); toolTipText = "Open $name in browser"
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { BrowserUtil.browse(url) }
                override fun mouseEntered(e: MouseEvent) { foreground = JBUI.CurrentTheme.Link.Foreground.HOVERED }
                override fun mouseExited(e: MouseEvent) { foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED }
            })
        }
    }

    private fun loadCommits() { thread {
        try { commits = runBlocking { provider.getPullRequestCommits(owner, repo, pr.number) }; SwingUtilities.invokeLater { commitsLoadingLabel.isVisible = false; renderCommits() } }
        catch (_: Exception) { SwingUtilities.invokeLater { commitsLoadingLabel.text = "Failed to load commits"; commitsLoadingLabel.foreground = UIUtil.getErrorForeground() } }
    } }

    private fun renderCommits() {
        commitsPanel.removeAll()
        if (commits.isEmpty()) { commitsPanel.add(JBLabel("No commits found").apply { font = sf; foreground = UIUtil.getContextHelpForeground(); border = JBUI.Borders.empty(4, 0); alignmentX = Component.LEFT_ALIGNMENT }) }
        else { commits.forEach { commit ->
            val row = JPanel(FlowLayout(FlowLayout.LEFT, 6, 3)).apply { isOpaque = false; alignmentX = Component.LEFT_ALIGNMENT }
            val hashLabel = JBLabel(commit.sha.take(7)).apply {
                font = Font(Font.MONOSPACED, Font.PLAIN, 11); foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR); toolTipText = "Open commit in browser"
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) { if (commit.url.isNotBlank()) BrowserUtil.browse(commit.url) }
                    override fun mouseEntered(e: MouseEvent) { foreground = JBUI.CurrentTheme.Link.Foreground.HOVERED }
                    override fun mouseExited(e: MouseEvent) { foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED }
                })
            }
            row.add(hashLabel)
            row.add(JBLabel(commit.message).apply { font = Font(Font.MONOSPACED, Font.PLAIN, 11); foreground = UIUtil.getLabelForeground() })
            commitsPanel.add(row)
        } }
        commitsPanel.revalidate(); commitsPanel.repaint()
    }

    private fun postComment() { val t = commentTA.text.trim(); if (t.isEmpty()) return; bg({ provider.addIssueComment(owner, repo, pr.number, t) }) { commentTA.text = ""; loadComments() } }
    private fun loadComments() { thread { try { comments = runBlocking { provider.getIssueComments(owner, repo, pr.number) }; SwingUtilities.invokeLater { renderComments() } } catch (_: Exception) {} } }
    private fun renderComments() {
        commentsPanel.removeAll()
        comments.forEach { c ->
            val rendered = MarkdownRenderer.render(c.body)
            val bodyPane = JEditorPane().apply {
                contentType = "text/html"; isEditable = false; background = cardBg
                font = JBUI.Fonts.label(); putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
                text = "<html><body style='${MarkdownRenderer.pageCss(uiFontSize)}'>$rendered</body></html>"; border = JBUI.Borders.empty(2, 0)
                addHyperlinkListener { event -> if (event.eventType == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) { BrowserUtil.browse(event.url.toString()) } }
            }
            SwingUtilities.invokeLater {
                val pw = bodyPane.parent?.width ?: return@invokeLater
                if (bodyPane.width != pw) {
                    bodyPane.setSize(pw, Int.MAX_VALUE)
                    val pref = bodyPane.preferredSize
                    bodyPane.minimumSize = Dimension(pw, pref.height)
                    bodyPane.revalidate()
                }
            }
            val cardContent = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS); isOpaque = false
                add(JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
                    isOpaque = false
                    add(JBLabel(c.author).apply { font = JBUI.Fonts.label(12f).asBold() })
                    add(JBLabel(fmt(c.createdAt)).apply { font = sf; foreground = UIUtil.getContextHelpForeground() })
                })
                add(Box.createVerticalStrut(4)); add(bodyPane)
            }
            val card = cardPanel(cardContent); card.alignmentX = Component.LEFT_ALIGNMENT
            commentsPanel.add(card); commentsPanel.add(Box.createVerticalStrut(6))
        }
        commentsPanel.revalidate(); commentsPanel.repaint()
    }

    private fun closePR() = bg({ provider.updateIssue(owner, repo, pr.number, state = "closed") }) { onRefresh() }
    private fun reopenPR() = bg({ provider.updateIssue(owner, repo, pr.number, state = "open") }) { onRefresh() }
    private fun bg(fn: suspend () -> Unit, onDone: () -> Unit) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Processing...", true) {
            override fun run(i: ProgressIndicator) { i.isIndeterminate = true; try { runBlocking { fn() }; ApplicationManager.getApplication().invokeLater { onDone() } } catch (e: Exception) { ApplicationManager.getApplication().invokeLater { JOptionPane.showMessageDialog(null, "Failed: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE) } } }
        })
    }

    companion object {
        fun fmt(iso: String): String = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val d = sdf.parse(iso.take(19)) ?: return iso
            val diff = Date().time - d.time; val m = diff / 60000; val h = m / 60; val dd = h / 24; val w = dd / 7
            when { m < 1 -> "now"; m < 60 -> "${m}m ago"; h < 24 -> "${h}h ago"; dd < 7 -> "${dd}d ago"; w < 5 -> "${w}w ago"; else -> SimpleDateFormat("MMM d", Locale.US).format(d) }
        } catch (_: Exception) { iso }
    }
}
