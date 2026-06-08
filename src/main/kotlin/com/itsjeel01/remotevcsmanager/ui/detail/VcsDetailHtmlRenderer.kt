package com.itsjeel01.remotevcsmanager.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import com.itsjeel01.remotevcsmanager.models.CommitSummary
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.providers.RemoteVcsProvider
import com.itsjeel01.remotevcsmanager.ui.components.HtmlSanitizer
import com.itsjeel01.remotevcsmanager.ui.components.JcefDiagnostics
import com.itsjeel01.remotevcsmanager.ui.TimeFormat
import com.itsjeel01.remotevcsmanager.ui.blend
import com.itsjeel01.remotevcsmanager.ui.theme.LocalPlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.LocalThemeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val LOG = Logger.getInstance("VcsDetailHtmlRenderer")





/**
 * Renders VCS description + comments in a **single** JCEF browser instance.
 *
 * Content items (description first, then comments in order) are converted
 * to HTML via the provider's markdown API, wrapped in themed "bubble"
 * containers that mirror Compose Card styling, and loaded into one
 * Chromium instance. Updates reactively as comments arrive.
 */
@Composable
fun VcsDetailHtmlRenderer(
    description: String?,
    comments: List<IssueComment>,
    provider: RemoteVcsProvider,
    context: String,
    modifier: Modifier = Modifier,
    commits: List<CommitSummary> = emptyList()
) {
    val themeColors = LocalThemeColors.current
    val platformFonts = LocalPlatformFonts.current


    var descriptionHtml by remember { mutableStateOf<String?>(null) }
    var commentHtmls by remember { mutableStateOf<List<String?>>(emptyList()) }
    var commitHtmls by remember { mutableStateOf<List<String?>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasEverLoaded by remember { mutableStateOf(false) }
    var loadingPhase by remember { mutableStateOf("Starting…") }
    var loadingDetail by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }


    var browser by remember { mutableStateOf<JBCefBrowser?>(null) }
    var browserError by remember { mutableStateOf<String?>(null) }

    // Create browser eagerly before first render
    LaunchedEffect(Unit) {
        if (browser == null && browserError == null) {
            val result = JcefDiagnostics.createBrowser()
            browser = result.browser
            if (browser == null) {
                browserError = result.diagnostics.joinToString("\n")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            browser?.let {
                try { Disposer.dispose(it) } catch (_: Exception) {}
            }
        }
    }




    LaunchedEffect(description, comments, commits, context, browser) {
        isLoading = true
        loadingPhase = "Rendering description…"
        loadingDetail = null
        error = null

        try {
            if (!description.isNullOrBlank()) {
                loadingPhase = "Rendering description…"
                val html = withContext(Dispatchers.IO) {
                    val rendered = provider.renderMarkdown(description, context)
                    if (rendered == description) {
                        "<pre>${description.escapeHtml()}</pre>"
                    } else {
                        rendered
                    }
                }
                descriptionHtml = html
                loadHtml(browser, descriptionHtml, emptyList(), comments, emptyList(), commits)
            } else {
                descriptionHtml = null
                loadHtml(browser, null, emptyList(), comments, emptyList(), commits)
            }
            isLoading = false
            hasEverLoaded = true

            if (comments.isNotEmpty()) {
                val rendered = Array<String?>(comments.size) { null }
                for (i in comments.indices) {
                    loadingPhase = "Rendering comments…"
                    loadingDetail = "${i + 1} of ${comments.size}"
                    val c = comments[i]
                    val html = withContext(Dispatchers.IO) {
                        val r = provider.renderMarkdown(c.body, context)
                        if (r == c.body) "<pre>${c.body.escapeHtml()}</pre>" else r
                    }
                    rendered[i] = html
                    commentHtmls = rendered.toList()
                    loadHtml(browser, descriptionHtml, commentHtmls, comments, commitHtmls, commits)
                }
                loadingDetail = null
            }

            if (commits.isNotEmpty()) {
                loadingPhase = "Rendering commits…"
                val rendered = Array<String?>(commits.size) { null }
                for (i in commits.indices) {
                    rendered[i] = commits[i].message
                    commitHtmls = rendered.toList()
                    loadHtml(browser, descriptionHtml, commentHtmls, comments, commitHtmls, commits)
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            LOG.warn("Failed to render detail content", e)
            error = e.message ?: "Render failed"
            isLoading = false
        }
        loadingPhase = ""
    }

    val isBright = JBColor.isBright()
    LaunchedEffect(isBright) {
        if (browser != null && (descriptionHtml != null || commentHtmls.isNotEmpty() || commitHtmls.isNotEmpty())) {
            loadHtml(browser, descriptionHtml, commentHtmls, comments, commitHtmls, commits)
        }
    }


    Box(modifier = modifier.fillMaxSize()) {
        if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(themeColors.Bg.primary)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("⚠ $error", fontSize = platformFonts.small, color = themeColors.Text.error)
            }
        } else {
            if (hasEverLoaded) {
                SwingPanel(
                    background = themeColors.Bg.primary,
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        browser?.component
                            ?: javax.swing.JLabel(
                                "<html><div style='padding:16px;font-size:11px;font-family:monospace'>" +
                                (browserError ?: "Initializing…").replace("\n", "<br>") +
                                "</div></html>"
                            ).apply {
                                foreground = JBColor.foreground()
                                background = JBColor.PanelBackground
                                isOpaque = true
                                verticalAlignment = javax.swing.SwingConstants.TOP
                                verticalTextPosition = javax.swing.SwingConstants.TOP
                            }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(themeColors.Bg.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = themeColors.Text.link,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            loadingPhase,
                            fontSize = platformFonts.small,
                            color = themeColors.Text.secondary
                        )
                        val detail = loadingDetail
                        if (detail != null) {
                            Text(
                                detail,
                                fontSize = platformFonts.xsmall,
                                color = themeColors.Text.disabled
                            )
                        }
                    }
                }
            }
        }
    }
}





/**
 * Build the full themed HTML document from description + comment bubbles
 * and load it into the JCEF browser.
 */
private fun loadHtml(
    browser: JBCefBrowser?,
    descriptionHtml: String?,
    commentHtmls: List<String?>?,
    comments: List<IssueComment>,
    commitHtmls: List<String?>? = emptyList(),
    commits: List<CommitSummary> = emptyList()
) {
    if (browser == null) return

    val bubbles = buildString {

        if (descriptionHtml != null) {
            append("""
                <div class="vcs-bubble">
                    <div class="vcs-bubble-label">Description</div>
                    <div class="vcs-bubble-body">${HtmlSanitizer.sanitize(descriptionHtml)}</div>
                </div>
            """.trimIndent())
        }

        val ch = commitHtmls ?: emptyList()
        if (ch.isNotEmpty()) {
            append("""
                <div class="vcs-section-header">Commits (${ch.count { it != null }})</div>
            """.trimIndent())
            ch.forEachIndexed { i, _ ->
                if (i < commits.size) {
                    val cmt = commits[i]
                    val sha = cmt.sha.take(7)
                    val msg = cmt.message.escapeHtml()
                    val author = cmt.author?.escapeHtml() ?: ""
                    val time = cmt.createdAt?.let { TimeFormat.relative(it) } ?: ""
                    append("""
                        <div class="vcs-commit">
                            <div class="vcs-commit-line"></div>
                            <div class="vcs-commit-dot"></div>
                            <div class="vcs-commit-body">
                                <a href="${cmt.url.escapeHtml()}" class="vcs-commit-sha">$sha</a>
                                <span class="vcs-commit-msg">$msg</span>
                                <span class="vcs-commit-meta">$author · $time</span>
                            </div>
                        </div>
                    """.trimIndent())
                }
            }
        }

        val htmls = commentHtmls ?: emptyList()
        if (htmls.isNotEmpty()) {
            val renderedCount = htmls.count { it != null }
            if (renderedCount > 0) {
                append("""
                    <div class="vcs-section-header">Comments ($renderedCount)</div>
                """.trimIndent())
            }
            htmls.forEachIndexed { i, html ->
                if (html != null) {
                    val comment = comments.getOrNull(i)
                    val author = comment?.author?.escapeHtml() ?: "unknown"
                    val time = comment?.createdAt?.let { TimeFormat.relative(it) } ?: ""
                    append("""
                        <div class="vcs-bubble">
                            <div class="vcs-bubble-header">
                                <span class="vcs-author">$author</span>
                                <span class="vcs-time">$time</span>
                            </div>
                            <div class="vcs-bubble-body">${HtmlSanitizer.sanitize(html)}</div>
                        </div>
                    """.trimIndent())
                }
            }
        }

        val pending = htmls.count { it == null }
        if (pending > 0) {
            append("""
                <div class="vcs-loading">Loading $pending more comments…</div>
            """.trimIndent())
        }
    }

    val themed = buildThemedPage(bubbles)
    try {
        browser.loadHTML(themed)
    } catch (e: Exception) {
        LOG.warn("Failed to load HTML into JCEF", e)
    }
}





private fun buildThemedPage(bodyHtml: String): String {
    val bg = ColorUtil.toHtmlColor(JBColor.PanelBackground)
    val fg = ColorUtil.toHtmlColor(JBColor.foreground())
    val link = ColorUtil.toHtmlColor(JBUI.CurrentTheme.Link.Foreground.ENABLED)
    val linkHover = ColorUtil.toHtmlColor(JBUI.CurrentTheme.Link.Foreground.HOVERED)
    val border = ColorUtil.toHtmlColor(JBColor.border())
    val secondary = ColorUtil.toHtmlColor(blend(JBColor.foreground(), JBColor.PanelBackground, 0.55f))
    val linkAlpha = JBUI.CurrentTheme.Link.Foreground.ENABLED
    val focusRing = ColorUtil.toHtmlColor(java.awt.Color(linkAlpha.red, linkAlpha.green, linkAlpha.blue, 76))
    val isBright = JBColor.isBright()


    val bubbleBg = if (isBright) "#FFFFFF" else "#1C2128"
    val bubbleBorder = border
    val bubbleLabelFg = secondary


    val codeBg = if (isBright) "#F0F2F5" else "#2D333B"
    val codeBlockBg = if (isBright) "#F6F8FA" else "#1C2128"


    val tableStripe = if (isBright) "#F6F8FA" else "#22272E"


    val quoteBorder = if (isBright) "#D0D7DE" else "#444C56"
    val quoteFg = if (isBright) "#656D76" else "#8B949E"


    val inputBg = if (isBright) "#FFFFFF" else "#22272E"
    val inputBorder = if (isBright) "#D0D7DE" else "#444C56"
    val shadow = if (isBright) "rgba(31,35,40,0.08)" else "rgba(1,4,9,0.3)"


    val markBg = if (isBright) "#FFF8C5" else "#5B4B00"
    val kbdBg = if (isBright) "#F6F8FA" else "#272B33"
    val kbdBorder = if (isBright) "#D0D7DE" else "#444C56"

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                                                                                   
                   Theme CSS — mirrors active JetBrains IDE theme
                   ================================================================ */
                *, *::before, *::after { box-sizing: border-box; }

                :root {
                    --bg: $bg; --fg: $fg; --link: $link; --link-hover: $linkHover;
                    --border: $border; --secondary: $secondary;
                    --code-bg: $codeBg; --code-block-bg: $codeBlockBg;
                    --table-stripe: $tableStripe;
                    --quote-border: $quoteBorder; --quote-fg: $quoteFg;
                    --input-bg: $inputBg; --input-border: $inputBorder;
                    --focus-ring: $focusRing; --shadow: $shadow;
                    --mark-bg: $markBg;
                    --kbd-bg: $kbdBg; --kbd-border: $kbdBorder;
                    --bubble-bg: $bubbleBg; --bubble-border: $bubbleBorder;
                    --bubble-label-fg: $bubbleLabelFg;
                }

                html { font-size: 13px; -webkit-text-size-adjust: 100%; }
                body {
                    margin: 0; padding: 8px 12px;
                    background-color: var(--bg); color: var(--fg);
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI",
                                 Helvetica, Arial, sans-serif;
                    font-size: 1rem; line-height: 1.6;
                    word-wrap: break-word; overflow-wrap: break-word;
                    -webkit-font-smoothing: antialiased;
                }

                                                                         
                .vcs-bubble {
                    background: var(--bubble-bg);
                    border: 1px solid var(--bubble-border);
                    border-radius: 8px;
                    padding: 10px 12px;
                    margin-bottom: 10px;
                }
                .vcs-bubble-label {
                    font-size: 0.8em; font-weight: 700;
                    color: var(--bubble-label-fg);
                    margin-bottom: 6px;
                    text-transform: uppercase; letter-spacing: 0.05em;
                }
                .vcs-bubble-header {
                    display: flex; gap: 10px; align-items: baseline;
                    margin-bottom: 6px;
                }
                .vcs-author {
                    font-weight: 700; color: var(--fg); font-size: 0.95em;
                }
                .vcs-time {
                    font-size: 0.8em; color: var(--secondary);
                }
                .vcs-bubble-body {
                    color: var(--fg);
                }
                .vcs-bubble-body > :first-child { margin-top: 0; }
                .vcs-bubble-body > :last-child { margin-bottom: 0; }

                .vcs-section-header {
                    font-weight: 700; color: var(--secondary);
                    font-size: 0.95em; margin: 16px 0 8px 0;
                }
                .vcs-loading {
                    color: var(--secondary); font-size: 0.85em;
                    font-style: italic; padding: 8px 0;
                }

                
                .vcs-commit {
                    position: relative;
                    padding: 4px 0 4px 24px;
                    margin-bottom: 2px;
                }
                .vcs-commit-line {
                    position: absolute;
                    left: 8px;
                    top: 16px;
                    bottom: -4px;
                    width: 2px;
                    background: var(--border);
                    border-radius: 1px;
                }
                .vcs-commit:last-child .vcs-commit-line {
                    display: none;
                }
                .vcs-commit-dot {
                    position: absolute;
                    left: 4px;
                    top: 8px;
                    width: 10px;
                    height: 10px;
                    border-radius: 50%;
                    background: var(--link);
                    border: 2px solid var(--bubble-bg);
                    z-index: 1;
                }
                .vcs-commit-body {
                    display: flex;
                    gap: 8px;
                    align-items: baseline;
                    flex-wrap: wrap;
                }
                .vcs-commit-sha {
                    font-family: "JetBrains Mono", Menlo, Monaco, Consolas, monospace;
                    font-size: 0.85em;
                    font-weight: 600;
                    color: var(--link);
                    text-decoration: none;
                }
                .vcs-commit-sha:hover {
                    text-decoration: underline;
                }
                .vcs-commit-msg {
                    font-size: 0.95em;
                    color: var(--fg);
                    flex: 1;
                    min-width: 0;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }
                .vcs-commit-meta {
                    font-size: 0.8em;
                    color: var(--secondary);
                    white-space: nowrap;
                }

                                                                         
                h1,h2,h3,h4,h5,h6 {
                    margin-top: 16px; margin-bottom: 10px;
                    font-weight: 600; line-height: 1.25; color: var(--fg);
                }
                h1 { font-size: 1.75em; border-bottom: 1px solid var(--border); padding-bottom: .3em; }
                h2 { font-size: 1.4em; border-bottom: 1px solid var(--border); padding-bottom: .3em; }
                h3 { font-size: 1.15em; }
                h4 { font-size: 1em; }
                h5 { font-size: .875em; }
                h6 { font-size: .85em; color: var(--secondary); }
                p { margin-top: 0; margin-bottom: 8px; }

                                                                         
                a { color: var(--link); text-decoration: none; }
                a:hover, a:focus { color: var(--link-hover); text-decoration: underline; outline: none; }

                                                                         
                code {
                    font-family: "JetBrains Mono", Menlo, Monaco, Consolas, monospace;
                    font-size: 0.9em; background-color: var(--code-bg);
                    padding: 0.2em 0.4em; border-radius: 3px;
                }
                pre {
                    background-color: var(--code-block-bg); padding: 12px;
                    border-radius: 6px; overflow-x: auto;
                    margin-top: 0; margin-bottom: 12px; line-height: 1.45;
                }
                pre code { background: transparent; padding: 0; border-radius: 0; font-size: 0.92em; }

                                                                         
                blockquote {
                    margin: 0 0 12px 0; padding: 0 1em;
                    color: var(--quote-fg); border-left: 4px solid var(--quote-border);
                }
                blockquote > :first-child { margin-top: 0; }
                blockquote > :last-child { margin-bottom: 0; }

                                                                         
                table { border-collapse: collapse; width: 100%; margin-bottom: 12px; }
                th, td { border: 1px solid var(--border); padding: 6px 12px; text-align: left; }
                th { font-weight: 600; background: var(--table-stripe); }
                tr:nth-child(even) td { background: var(--table-stripe); }

                                                                         
                ul, ol { margin-top: 0; margin-bottom: 12px; padding-left: 2em; }
                li { margin-bottom: 2px; }
                .task-list-item { list-style-type: none; margin-left: -1.5em; }
                .task-list-item input[type="checkbox"] { margin-right: 6px; vertical-align: middle; }

                                                                         
                hr { border: 0; border-bottom: 1px solid var(--border); margin: 16px 0; }

                                                                         
                img, video, svg { max-width: 100%; height: auto !important; border-radius: 6px; }
                img { display: inline-block; vertical-align: middle; }
                figure { margin: 0 0 12px 0; text-align: center; }
                figcaption { font-size: 0.85em; color: var(--secondary); margin-top: 4px; }
                audio, video, canvas { max-width: 100%; display: block; }
                iframe { max-width: 100%; border: 1px solid var(--border); border-radius: 6px; }
                object, embed { max-width: 100%; }

                                                                         
                details {
                    border: 1px solid var(--border); border-radius: 6px;
                    padding: 6px 12px; margin-bottom: 12px;
                }
                details[open] { padding-bottom: 12px; }
                summary { cursor: pointer; font-weight: 600; padding: 6px 0; outline: none; }
                summary:hover { color: var(--link); }

                                                                         
                form { margin-bottom: 12px; }
                fieldset { border: 1px solid var(--border); border-radius: 6px; padding: 10px 14px; margin-bottom: 12px; }
                legend { font-weight: 600; padding: 0 6px; }
                label { font-weight: 500; display: inline-block; margin-bottom: 4px; }
                input, textarea, select, button { font-family: inherit; font-size: inherit; color: var(--fg); }
                input[type="text"], input[type="email"], input[type="password"],
                input[type="url"], input[type="number"], input[type="search"],
                textarea, select {
                    background: var(--input-bg); border: 1px solid var(--input-border);
                    border-radius: 6px; padding: 5px 12px; outline: none;
                }
                input:focus, textarea:focus, select:focus {
                    border-color: var(--focus-ring);
                    box-shadow: 0 0 0 3px var(--shadow);
                }
                input[type="checkbox"], input[type="radio"] { accent-color: var(--link); margin-right: 4px; }
                button, input[type="button"], input[type="submit"], input[type="reset"] {
                    background: var(--code-bg); border: 1px solid var(--input-border);
                    border-radius: 6px; padding: 5px 16px; cursor: pointer; font-weight: 500;
                }
                button:hover { background: var(--border); }

                                                                         
                strong, b { font-weight: 600; }
                em, i { font-style: italic; }
                u { text-decoration: underline; }
                s, del, strike { text-decoration: line-through; }
                small { font-size: 0.85em; color: var(--secondary); }
                mark { background: var(--mark-bg); padding: 0.1em 0.2em; border-radius: 2px; }
                abbr[title] { text-decoration: underline dotted; cursor: help; }
                kbd {
                    background: var(--kbd-bg); border: 1px solid var(--kbd-border);
                    border-radius: 3px; padding: 0.1em 0.4em;
                    font-family: "JetBrains Mono", monospace; font-size: 0.85em;
                    box-shadow: inset 0 -1px 0 var(--kbd-border);
                }
                tt, samp, var { font-family: "JetBrains Mono", monospace; }
                var { font-style: italic; }
                ruby > rt { font-size: 0.6em; color: var(--secondary); }

                                                                         
                ::-webkit-scrollbar { width: 8px; height: 8px; }
                ::-webkit-scrollbar-track { background: transparent; }
                ::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }
                ::-webkit-scrollbar-thumb:hover { background: var(--secondary); }

                                                                         
                ::selection { background-color: var(--focus-ring); color: var(--bg); }

                                                                         
                @media print { body { background: white; color: black; } }
            </style>
        </head>
        <body>
            $bodyHtml
        </body>
        </html>
    """.trimIndent()
}





private fun String.escapeHtml(): String =
    this.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
