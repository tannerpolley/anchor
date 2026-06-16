package com.itsjeel01.remotevcsmanager.ui.detail

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.models.Label
import com.itsjeel01.remotevcsmanager.ui.TimeFormat
import com.itsjeel01.remotevcsmanager.ui.blend
import com.itsjeel01.remotevcsmanager.ui.components.HtmlSanitizer
import com.itsjeel01.remotevcsmanager.ui.components.JcefDiagnostics
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JTextArea

class SwingIssueDetailRenderer : Disposable {

    private val browser: JBCefBrowser?
    private val root = JBPanel<JBPanel<*>>(BorderLayout())

    val component: JComponent = root

    init {
        val result = JcefDiagnostics.createBrowser()
        browser = result.browser
        if (browser != null) {
            root.add(browser.component, BorderLayout.CENTER)
        } else {
            root.add(createMessagePanel(result.diagnostics.joinToString("\n")), BorderLayout.CENTER)
        }
        showPlaceholder("Select an issue to read its body and comments.")
    }

    fun showPlaceholder(message: String): Unit {
        loadPage(
            buildThemedPage(
                """
                    <section class="empty-state">
                        <div>${message.escapeHtml()}</div>
                    </section>
                """.trimIndent()
            )
        )
    }

    fun showLoading(issue: Issue): Unit {
        loadPage(
            buildThemedPage(
                """
                    <section class="issue-header">
                        <div class="issue-number">#${issue.number.toString().escapeHtml()}</div>
                        <h1>${issue.title.escapeHtml()}</h1>
                        <div class="issue-meta">Loading issue details...</div>
                    </section>
                """.trimIndent()
            )
        )
    }

    fun showIssue(
        issue: Issue,
        comments: List<IssueComment>,
        renderedBody: String,
        renderedComments: List<String>
    ): Unit {
        val commentsHtml = comments.mapIndexed { index, comment ->
            val body = renderedComments.getOrNull(index).orEmpty()
            commentSection(comment, body)
        }.joinToString("\n")

        loadPage(
            buildThemedPage(
                """
                    <section class="issue-header">
                        <div class="issue-number">#${issue.number.toString().escapeHtml()}</div>
                        <h1>${issue.title.escapeHtml()}</h1>
                        <div class="issue-meta">
                            Opened by ${issue.author.escapeHtml()} · updated ${TimeFormat.relative(issue.updatedAt).escapeHtml()}
                        </div>
                        ${labels(issue.labels)}
                    </section>
                    <section class="timeline-item">
                        <div class="timeline-title">Description</div>
                        <div class="markdown-body">${HtmlSanitizer.sanitize(renderedBody)}</div>
                    </section>
                    <section class="comments-header">Comments (${comments.size})</section>
                    $commentsHtml
                """.trimIndent()
            )
        )
    }

    fun showError(issue: Issue?, message: String): Unit {
        val title = issue?.let { "#${it.number} ${it.title}" } ?: "GitHub Issue"
        loadPage(
            buildThemedPage(
                """
                    <section class="issue-header">
                        <h1>${title.escapeHtml()}</h1>
                        <div class="issue-meta error">${message.escapeHtml()}</div>
                    </section>
                """.trimIndent()
            )
        )
    }

    override fun dispose(): Unit {
        browser?.let { Disposer.dispose(it) }
    }

    private fun commentSection(comment: IssueComment, body: String): String =
        """
            <section class="timeline-item">
                <div class="timeline-title">
                    ${comment.author.escapeHtml()}
                    <span>${TimeFormat.relative(comment.createdAt).escapeHtml()}</span>
                </div>
                <div class="markdown-body">${HtmlSanitizer.sanitize(body)}</div>
            </section>
        """.trimIndent()

    private fun labels(labels: List<Label>): String {
        if (labels.isEmpty()) return ""
        return labels.joinToString("", prefix = "<div class=\"labels\">", postfix = "</div>") { label ->
            val color = label.color.takeIf { it.matches(Regex("[0-9a-fA-F]{6}")) } ?: "888888"
            "<span class=\"label\" style=\"border-color:#$color\">${label.name.escapeHtml()}</span>"
        }
    }

    private fun loadPage(html: String): Unit {
        browser?.loadHTML(html)
    }

    private fun createMessagePanel(message: String): JComponent {
        val area = JTextArea(message)
        area.isEditable = false
        area.isOpaque = false
        area.lineWrap = true
        area.wrapStyleWord = true
        area.border = JBUI.Borders.empty(12)
        return JBScrollPane(area)
    }

    private fun buildThemedPage(body: String): String {
        val background = ColorUtil.toHtmlColor(JBColor.PanelBackground)
        val foreground = ColorUtil.toHtmlColor(JBColor.foreground())
        val border = ColorUtil.toHtmlColor(JBColor.border())
        val secondary = ColorUtil.toHtmlColor(blend(JBColor.foreground(), JBColor.PanelBackground, 0.58f))
        val muted = ColorUtil.toHtmlColor(blend(JBColor.foreground(), JBColor.PanelBackground, 0.38f))
        val link = ColorUtil.toHtmlColor(JBUI.CurrentTheme.Link.Foreground.ENABLED)
        val codeBackground = if (JBColor.isBright()) "#F6F8FA" else "#20242B"
        val itemBackground = if (JBColor.isBright()) "#FFFFFF" else "#1B1D22"

        return """
            <!doctype html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    :root {
                        --bg: $background;
                        --fg: $foreground;
                        --border: $border;
                        --secondary: $secondary;
                        --muted: $muted;
                        --link: $link;
                        --code-bg: $codeBackground;
                        --item-bg: $itemBackground;
                    }
                    * { box-sizing: border-box; }
                    body {
                        margin: 0;
                        padding: 14px;
                        background: var(--bg);
                        color: var(--fg);
                        font: 13px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                        line-height: 1.5;
                    }
                    a { color: var(--link); text-decoration: none; }
                    a:hover { text-decoration: underline; }
                    .issue-header {
                        padding: 0 2px 12px;
                        border-bottom: 1px solid var(--border);
                        margin-bottom: 12px;
                    }
                    .issue-number {
                        color: var(--secondary);
                        font-size: 12px;
                        margin-bottom: 4px;
                    }
                    h1 {
                        margin: 0 0 6px;
                        font-size: 18px;
                        line-height: 1.25;
                        font-weight: 600;
                    }
                    .issue-meta, .timeline-title span {
                        color: var(--secondary);
                        font-size: 12px;
                    }
                    .error { color: #D1242F; }
                    .labels {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 5px;
                        margin-top: 9px;
                    }
                    .label {
                        border: 1px solid var(--border);
                        border-radius: 999px;
                        padding: 1px 7px;
                        font-size: 11px;
                        color: var(--fg);
                    }
                    .timeline-item {
                        background: var(--item-bg);
                        border: 1px solid var(--border);
                        border-radius: 6px;
                        margin: 0 0 12px;
                        overflow: hidden;
                    }
                    .timeline-title {
                        padding: 8px 10px;
                        border-bottom: 1px solid var(--border);
                        font-weight: 600;
                        display: flex;
                        gap: 8px;
                        align-items: baseline;
                    }
                    .markdown-body {
                        padding: 10px;
                        overflow-wrap: anywhere;
                    }
                    .markdown-body > :first-child { margin-top: 0; }
                    .markdown-body > :last-child { margin-bottom: 0; }
                    .markdown-body p, .markdown-body ul, .markdown-body ol,
                    .markdown-body blockquote, .markdown-body pre, .markdown-body table {
                        margin: 0 0 12px;
                    }
                    .markdown-body code {
                        background: var(--code-bg);
                        border-radius: 4px;
                        padding: 1px 4px;
                        font-family: "JetBrains Mono", Consolas, monospace;
                        font-size: 12px;
                    }
                    .markdown-body pre {
                        background: var(--code-bg);
                        border: 1px solid var(--border);
                        border-radius: 6px;
                        padding: 10px;
                        overflow: auto;
                    }
                    .markdown-body pre code {
                        background: transparent;
                        padding: 0;
                    }
                    .markdown-body blockquote {
                        border-left: 3px solid var(--border);
                        color: var(--secondary);
                        padding-left: 10px;
                    }
                    .markdown-body table {
                        border-collapse: collapse;
                        width: 100%;
                    }
                    .markdown-body th, .markdown-body td {
                        border: 1px solid var(--border);
                        padding: 6px 8px;
                    }
                    .markdown-body img {
                        max-width: 100%;
                    }
                    .comments-header {
                        margin: 16px 2px 8px;
                        color: var(--muted);
                        font-weight: 600;
                    }
                    .empty-state {
                        height: 100vh;
                        display: grid;
                        place-items: center;
                        color: var(--secondary);
                    }
                </style>
            </head>
            <body>$body</body>
            </html>
        """.trimIndent()
    }

    private fun String.escapeHtml(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
