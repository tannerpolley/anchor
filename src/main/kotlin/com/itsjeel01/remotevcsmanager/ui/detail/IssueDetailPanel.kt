package com.itsjeel01.remotevcsmanager.ui.detail

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
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import com.itsjeel01.remotevcsmanager.ui.components.StateBadge
import kotlinx.coroutines.runBlocking
import java.awt.*
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import kotlin.concurrent.thread

/**
 * Issue detail panel — monospace body, no HTML rendering, flat buttons, no duplicate back.
 */
class IssueDetailPanel(
    private val provider: GitHubProvider,
    private val owner: String,
    private val repo: String,
    private val issue: Issue,
    private val onBack: () -> Unit,
    private val onRefresh: () -> Unit
) : JPanel(BorderLayout()) {

    private var comments: List<IssueComment> = emptyList()
    private val monoFont = Font(Font.MONOSPACED, Font.PLAIN, JBUI.Fonts.label().size - 1)
    private val smallMono = Font(Font.MONOSPACED, Font.PLAIN, 10)

    // Title
    private val titleLabel = JBLabel().apply { font = JBUI.Fonts.label(14f).asBold(); cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR) }
    private val titleField = JBTextField().apply { isVisible = false; font = monoFont }

    // Body — monospace JBTextArea (no HTML, clean native rendering)
    private val bodyArea = JBTextArea().apply {
        isEditable = false; lineWrap = true; wrapStyleWord = true; font = monoFont
        background = UIUtil.getPanelBackground()
    }

    // Comments
    private val commentsPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }

    // Comment input
    private val commentTA = JBTextArea().apply { emptyText.text = "Leave a comment..."; lineWrap = true; rows = 3; font = monoFont }

    init { layout = BorderLayout(); border = JBUI.Borders.empty(); buildUI(); loadComments() }

    private fun buildUI() {
        // ══ Top — state badge + actions (NO duplicate back button — tool window provides it) ══
        val badge = StateBadge().apply { setIssueState(issue.state) }
        val actions = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 2)).apply { isOpaque = false
            add(badge)
            add(flatBtn(if (issue.state == IssueState.OPEN) "Close" else "Reopen") { if (issue.state == IssueState.OPEN) closeIssue() else reopenIssue() })
            add(flatIconBtn(com.intellij.icons.AllIcons.Ide.External_link_arrow, "Open in GitHub") { BrowserUtil.browse(issue.url) })
        }
        add(JPanel(BorderLayout()).apply { background = UIUtil.getTableBackground(); border = JBUI.Borders.empty(2, 12, 1, 12)
            add(JPanel().apply { isOpaque = false }, BorderLayout.WEST) // spacer to match back button indentation
            add(actions, BorderLayout.EAST)
        }, BorderLayout.NORTH)

        // ══ Body — title + meta + description + comments ══
        titleLabel.text = "#${issue.number}  ${issue.title}"
        titleLabel.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) { if (e.clickCount == 2) startEditTitle() }
        })
        titleField.addActionListener { saveTitle() }

        bodyArea.text = issue.body ?: "No description provided."
        if (issue.body == null) bodyArea.foreground = UIUtil.getContextHelpForeground()

        val meta = JPanel(FlowLayout(FlowLayout.LEFT, 4, 1)).apply { isOpaque = false
            add(JBLabel(issue.author).apply { font = JBUI.Fonts.smallFont(); foreground = UIUtil.getContextHelpForeground() })
            add(JBLabel("· ${fmt(issue.createdAt)}").apply { font = JBUI.Fonts.smallFont(); foreground = UIUtil.getContextHelpForeground() })
            if (issue.labels.isNotEmpty()) add(JBLabel("· ${issue.labels.take(5).joinToString(", ")}").apply { font = JBUI.Fonts.smallFont(); foreground = UIUtil.getContextHelpForeground() })
        }

        val section = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(titleLabel); add(titleField); add(meta)
            add(JBScrollPane(bodyArea).apply { border = JBUI.Borders.empty(6, 0); preferredSize = Dimension(100, 120) })
            add(JBLabel("Comments (${issue.commentsCount})").apply { font = JBUI.Fonts.label().asBold(); border = JBUI.Borders.empty(8, 0, 4, 0) })
            add(JBScrollPane(commentsPanel).apply { preferredSize = Dimension(100, 100) })
        }

        add(JBScrollPane(section).apply { border = JBUI.Borders.empty(0, 12, 0, 12) }, BorderLayout.CENTER)

        // ══ Comment input at bottom ══
        val postBtn = flatBtn("Post Comment") { postComment() }
        add(JPanel(BorderLayout()).apply { border = JBUI.Borders.empty(4, 12, 8, 12)
            add(JBScrollPane(commentTA).apply { preferredSize = Dimension(100, 56) }, BorderLayout.CENTER)
            add(postBtn, BorderLayout.EAST)
        }, BorderLayout.SOUTH)
    }

    private fun flatBtn(text: String, action: () -> Unit) = JButton(text).apply {
        font = JBUI.Fonts.smallFont(); isOpaque = false; border = JBUI.Borders.empty(2, 6); isBorderPainted = false
        addActionListener { action() }
    }

    private fun flatIconBtn(icon: javax.swing.Icon, tip: String, action: () -> Unit) = JButton(icon).apply {
        isOpaque = false; border = JBUI.Borders.empty(2, 4); isBorderPainted = false; toolTipText = tip
        addActionListener { action() }
    }

    private fun startEditTitle() {
        titleLabel.isVisible = false; titleField.text = issue.title; titleField.isVisible = true; titleField.selectAll(); titleField.requestFocusInWindow()
    }
    private fun saveTitle() {
        val nt = titleField.text.trim(); if (nt.isNotEmpty() && nt != issue.title) bg({ provider.updateIssue(owner, repo, issue.number, title = nt) }) { titleLabel.text = "#${issue.number} $nt"; onRefresh() }
        titleLabel.isVisible = true; titleField.isVisible = false
    }
    private fun closeIssue() = bg({ provider.closeIssue(owner, repo, issue.number) }) { onRefresh() }
    private fun reopenIssue() = bg({ provider.updateIssue(owner, repo, issue.number, state = "open") }) { onRefresh() }
    private fun postComment() { val t = commentTA.text.trim(); if (t.isEmpty()) return; bg({ provider.addIssueComment(owner, repo, issue.number, t) }) { commentTA.text = ""; onRefresh() } }

    private fun bg(fn: suspend () -> Unit, onDone: () -> Unit) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "", true) {
            override fun run(i: ProgressIndicator) { i.isIndeterminate = true; try { runBlocking { fn() }; ApplicationManager.getApplication().invokeLater { onDone() } } catch (e: Exception) { ApplicationManager.getApplication().invokeLater { JOptionPane.showMessageDialog(null, "Failed: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE) } } }
        })
    }

    private fun loadComments() {
        if (issue.commentsCount == 0) { commentsPanel.add(JBLabel("No comments.").apply { font = JBUI.Fonts.smallFont(); foreground = UIUtil.getContextHelpForeground(); border = JBUI.Borders.empty(4, 0) }); return }
        thread {
            try { comments = runBlocking { provider.getIssueComments(owner, repo, issue.number) }; SwingUtilities.invokeLater { renderComments() } } catch (_: Exception) {}
        }
    }

    private fun renderComments() {
        commentsPanel.removeAll()
        if (comments.isEmpty()) { commentsPanel.add(JBLabel("No comments.").apply { font = JBUI.Fonts.smallFont(); foreground = UIUtil.getContextHelpForeground(); border = JBUI.Borders.empty(4, 0) }) }
        else comments.forEachIndexed { idx, c ->
            val r = JPanel(BorderLayout()).apply { isOpaque = false; border = JBUI.Borders.empty(3, 0)
                add(JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply { isOpaque = false; add(JBLabel(c.author).apply { font = JBUI.Fonts.label(11f).asBold() }); add(JBLabel("· ${fmt(c.createdAt)}").apply { font = JBUI.Fonts.smallFont(); foreground = UIUtil.getContextHelpForeground() }) }, BorderLayout.NORTH)
                val body = JBTextArea().apply { isEditable = false; lineWrap = true; wrapStyleWord = true; font = monoFont; text = c.body; background = UIUtil.getPanelBackground(); border = JBUI.Borders.empty(2, 0) }
                add(body, BorderLayout.CENTER)
            }
            commentsPanel.add(r)
            if (idx < comments.size - 1) commentsPanel.add(JSeparator(SwingConstants.HORIZONTAL).apply { maximumSize = Dimension(Int.MAX_VALUE, 1) })
        }
        commentsPanel.revalidate(); commentsPanel.repaint()
    }

    companion object {
        fun fmt(iso: String): String = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val d = sdf.parse(iso.take(19)) ?: return iso; val diff = Date().time - d.time; val m = diff / 60000; val h = m / 60; val dd = h / 24; val w = dd / 7
            when { m < 1 -> "now"; m < 60 -> "${m}m ago"; h < 24 -> "${h}h ago"; dd < 7 -> "${dd}d ago"; w < 5 -> "${w}w ago"; else -> SimpleDateFormat("MMM d", Locale.US).format(d) }
        } catch (_: Exception) { iso }
    }
}
