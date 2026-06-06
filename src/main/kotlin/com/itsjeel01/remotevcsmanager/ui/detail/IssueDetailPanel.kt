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
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.models.IssueState
import com.itsjeel01.remotevcsmanager.models.Label
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

class IssueDetailPanel(
    private val provider: GitHubProvider,
    private val owner: String,
    private val repo: String,
    private val issue: Issue,
    private val onBack: () -> Unit,
    private val onRefresh: () -> Unit
) : JPanel(BorderLayout()) {

    private var comments: List<IssueComment> = emptyList()
    private val sf = JBUI.Fonts.smallFont()
    private val uiFontSize = JBUI.Fonts.label().size
    private val scrollPane = JBScrollPane().apply {
        border = JBUI.Borders.empty()
        verticalScrollBar.unitIncrement = 16
        viewport.isOpaque = false
    }

    private val titleLabel = JBLabel().apply {
        font = JBUI.Fonts.label(15f).asBold()
        horizontalAlignment = SwingConstants.LEFT
    }
    private val titleField = JBTextField().apply { isVisible = false }

    private val commentsPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS); isOpaque = false
    }

    private val commentTA = JBTextArea().apply {
        emptyText.text = "Leave a comment..."; lineWrap = true; wrapStyleWord = true; rows = 3
        font = JBUI.Fonts.label()
        border = BorderFactory.createCompoundBorder(JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1), JBUI.Borders.empty(8))
    }

    init { background = UIUtil.getTableBackground(); buildUI(); loadComments() }

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
        card.alignmentX = Component.LEFT_ALIGNMENT
        return card
    }

    private fun buildUI() {
        // Two-line header
        val header = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS); isOpaque = false
            border = BorderFactory.createCompoundBorder(JBUI.Borders.customLineBottom(UIUtil.getBoundsColor()), JBUI.Borders.empty(4, 10))
        }

        // Row 0: back + title + actions
        val row0 = JPanel(GridBagLayout()).apply { isOpaque = false; alignmentX = 0.0f }
        val backIcon = JLabel(AllIcons.Actions.Back).apply {
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR); toolTipText = "Back"
            border = JBUI.Borders.emptyRight(6)
            addMouseListener(object : MouseAdapter() { override fun mouseClicked(e: MouseEvent) { onBack() } })
        }
        titleLabel.text = issue.title

        // Filled action buttons using JPanel for zero outline
        val closeColor = if (issue.state == IssueState.OPEN) JBColor(0xCF222E, 0xF85149) else JBColor(0x2DA44E, 0x3FB950)
        val closeBtn = JPanel(GridBagLayout()).apply {
            isOpaque = true; background = closeColor; cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.empty(3, 10)
            add(JBLabel(if (issue.state == IssueState.OPEN) "Close" else "Reopen").apply { font = sf; foreground = Color.WHITE })
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { if (issue.state == IssueState.OPEN) closeIssue() else reopenIssue() }
                override fun mouseEntered(e: MouseEvent) { background = closeColor.darker() }
                override fun mouseExited(e: MouseEvent) { background = closeColor }
            })
        }
        val shareBtn = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false; cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            toolTipText = "Open in Browser"; border = JBUI.Borders.empty(3, 4)
            add(JBLabel(AllIcons.Ide.External_link_arrow))
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) { issue.url.let { BrowserUtil.browse(it) } }
            })
        }

        val gc = GridBagConstraints()
        gc.fill = GridBagConstraints.NONE; gc.anchor = GridBagConstraints.WEST; gc.insets = JBUI.emptyInsets()
        gc.gridx = 0; gc.weightx = 0.0; row0.add(backIcon, gc)
        gc.gridx = 1; gc.weightx = 1.0; gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = JBUI.insetsRight(8)
        row0.add(titleLabel, gc)
        gc.gridx = 2; gc.weightx = 0.0; gc.insets = JBUI.insetsRight(4); row0.add(shareBtn, gc)
        gc.gridx = 3; gc.weightx = 0.0; row0.add(closeBtn, gc)
        header.add(row0)

        // Row 1: metadata
        val metaRow = JPanel(FlowLayout(FlowLayout.LEFT, 3, 0)).apply {
            isOpaque = false; alignmentX = 0.0f
            add(StateBadge().apply { setIssueState(issue.state) })
            add(JBLabel(issue.author).apply { font = sf; foreground = UIUtil.getContextHelpForeground() })
            add(JBLabel(fmt(issue.createdAt)).apply { font = sf; foreground = UIUtil.getContextHelpForeground() })
            issue.labels.take(6).forEach { add(createLabelChip(it)) }
        }
        header.add(metaRow)
        add(header, BorderLayout.NORTH)

        // Scrollable body — no wrapper, viewport listener prevents centering
        val body = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS); isOpaque = false
            border = JBUI.Borders.empty(8, 12)
        }

        val rendered = MarkdownRenderer.render(issue.body ?: "_No description provided._")
        val descPane = JEditorPane().apply {
            contentType = "text/html"; isEditable = false; background = cardBg
            font = JBUI.Fonts.label(); putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            text = "<html><body style='${MarkdownRenderer.pageCss(uiFontSize)}'>$rendered</body></html>"
            alignmentX = Component.LEFT_ALIGNMENT
            addHyperlinkListener { event -> if (event.eventType == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) { BrowserUtil.browse(event.url.toString()) } }
        }
        // Post-layout: force JEditorPane to compute correct height at parent width
        SwingUtilities.invokeLater {
            val pw = descPane.parent?.width ?: return@invokeLater
            if (descPane.width != pw) {
                descPane.setSize(pw, Int.MAX_VALUE)
                val pref = descPane.preferredSize
                descPane.minimumSize = Dimension(pw, pref.height)
                descPane.revalidate()
            }
        }
        body.add(cardPanel(descPane))
        body.add(Box.createVerticalStrut(8))
        body.add(commentsPanel)
        body.add(Box.createVerticalGlue())

        scrollPane.setViewportView(body)
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
        // Keep content top-aligned: when viewport is taller than content, set body min height
        var inViewportResize = false
        scrollPane.viewport.addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent) {
                if (inViewportResize) return
                inViewportResize = true
                try {
                    val vph = scrollPane.viewport.height
                    val bodyPH = body.preferredSize.height
                    val newMinH = if (vph > bodyPH) vph else bodyPH
                    if (body.minimumSize.height != newMinH) {
                        body.minimumSize = Dimension(0, newMinH)
                        body.revalidate()
                    }
                } finally { inViewportResize = false }
            }
        })
        SwingUtilities.invokeLater { scrollPane.viewport.viewPosition = Point(0, 0) }
        add(scrollPane, BorderLayout.CENTER)

        // Comment input bar
        val postBtn = JButton("Post Comment").apply { addActionListener { postComment() } }
        add(JPanel(BorderLayout()).apply {
            isOpaque = false
            border = BorderFactory.createCompoundBorder(JBUI.Borders.customLineTop(UIUtil.getBoundsColor()), JBUI.Borders.empty(8, 12))
            add(commentTA, BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
                isOpaque = false; border = JBUI.Borders.emptyTop(6); add(postBtn)
            }, BorderLayout.SOUTH)
        }, BorderLayout.SOUTH)
    }

    private fun createLabelChip(label: Label): JPanel {
        val chipColor = try { Color(label.color.substring(0, 2).toInt(16), label.color.substring(2, 4).toInt(16), label.color.substring(4, 6).toInt(16)) } catch (_: Exception) { UIUtil.getBoundsColor() }
        val textColor = if ((chipColor.red*299+chipColor.green*587+chipColor.blue*114)/1000 > 140) Color.BLACK else Color.WHITE
        return JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = true; background = chipColor; border = JBUI.Borders.empty(2, 6)
            add(JBLabel(label.name).apply { font = sf; foreground = textColor })
        }
    }

    private fun startEditTitle() {
        titleLabel.isVisible = false; titleField.text = issue.title; titleField.isVisible = true; titleField.selectAll(); titleField.requestFocusInWindow()
    }
    private fun saveTitle() {
        val nt = titleField.text.trim(); if (nt.isNotEmpty() && nt != issue.title) bg({ provider.updateIssue(owner, repo, issue.number, title = nt) }) { titleLabel.text = nt; onRefresh() }
        titleLabel.isVisible = true; titleField.isVisible = false
    }
    private fun closeIssue() = bg({ provider.closeIssue(owner, repo, issue.number) }) { onRefresh() }
    private fun reopenIssue() = bg({ provider.updateIssue(owner, repo, issue.number, state = "open") }) { onRefresh() }
    private fun postComment() { val t = commentTA.text.trim(); if (t.isEmpty()) return; bg({ provider.addIssueComment(owner, repo, issue.number, t) }) { commentTA.text = ""; loadComments() } }
    private fun bg(fn: suspend () -> Unit, onDone: () -> Unit) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Processing...", true) {
            override fun run(i: ProgressIndicator) { i.isIndeterminate = true; try { runBlocking { fn() }; ApplicationManager.getApplication().invokeLater { onDone() } } catch (e: Exception) { ApplicationManager.getApplication().invokeLater { JOptionPane.showMessageDialog(null, "Failed: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE) } } }
        })
    }
    private fun loadComments() { thread { try { comments = runBlocking { provider.getIssueComments(owner, repo, issue.number) }; SwingUtilities.invokeLater { renderComments() } } catch (_: Exception) {} } }
    private fun renderComments() {
        commentsPanel.removeAll()
        comments.forEach { c ->
            val rendered = MarkdownRenderer.render(c.body)
            val bodyPane = JEditorPane().apply {
                contentType = "text/html"; isEditable = false; background = cardBg
                font = JBUI.Fonts.label(); putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
                text = "<html><body style='${MarkdownRenderer.pageCss(uiFontSize)}'>$rendered</body></html>"
                border = JBUI.Borders.empty(2, 0)
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

    companion object {
        fun fmt(iso: String): String = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val d = sdf.parse(iso.take(19)) ?: return iso
            val diff = Date().time - d.time; val m = diff / 60000; val h = m / 60; val dd = h / 24; val w = dd / 7
            when { m < 1 -> "now"; m < 60 -> "${m}m ago"; h < 24 -> "${h}h ago"; dd < 7 -> "${dd}d ago"; w < 5 -> "${w}w ago"; else -> SimpleDateFormat("MMM d", Locale.US).format(d) }
        } catch (_: Exception) { iso }
    }
}
