package com.itsjeel01.remotevcsmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.itsjeel01.remotevcsmanager.ui.theme.LocalPlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.LocalThemeColors
import com.itsjeel01.remotevcsmanager.ui.blend





/** Maximum HTML size we'll attempt to render in JCEF (5 MB).
 * Above this, we fall back to a plain-text preview to avoid
 * overwhelming Chromium's renderer. */
private const val MAX_HTML_SIZE_BYTES = 5 * 1024 * 1024

private val LOG = Logger.getInstance("VCSHtmlBrowser")





/**
 * Renders VCS-sourced HTML (descriptions, comments, READMEs) in an
 * IDE-theme-reactive embedded Chromium browser.
 *
 * Features:
 *  • Minimal JS-only sanitization — preserves all visual HTML
 *  • Full JetBrains theme mirroring via injected CSS
 *  • External link interception (opens in default browser)
 *  • Internal anchor navigation
 *  • Graceful fallback for missing/empty content
 *  • Proper JCEF lifecycle (no memory leaks)
 */
@Composable
fun VCSHtmlBrowser(
    rawHtml: String?,
    modifier: Modifier = Modifier,
    enableSanitization: Boolean = true
) {
    val themeColors = LocalThemeColors.current
    val platformFonts = LocalPlatformFonts.current


    val content = rawHtml?.trim()
    if (content.isNullOrBlank()) {
        EmptyContent(modifier)
        return
    }


    val sanitized = remember(content, enableSanitization) {
        val html = if (enableSanitization)
            HtmlSanitizer.sanitize(HtmlSanitizer.extractBody(content))
        else
            HtmlSanitizer.extractBody(content)
        html
    }


    val htmlBytes = sanitized.encodeToByteArray().size
    if (htmlBytes > MAX_HTML_SIZE_BYTES) {
        OversizeFallback(sanitized, modifier)
        return
    }


    var browser by remember { mutableStateOf<JBCefBrowser?>(null) }
    var browserError by remember { mutableStateOf<String?>(null) }


    DisposableEffect(Unit) {
        onDispose {
            browser?.let {
                try { Disposer.dispose(it) } catch (_: Exception) {}
            }
        }
    }


    val isBright = JBColor.isBright()


    LaunchedEffect(browser, sanitized, isBright) {
        val b = browser ?: return@LaunchedEffect
        try {
            b.loadHTML(buildThemedPage(sanitized))
        } catch (e: Exception) {
            LOG.warn("VCSHtmlBrowser: failed to load HTML", e)
        }
    }


    SwingPanel(
        background = themeColors.Bg.primary,
        modifier = modifier.fillMaxSize(),
        factory = {
            if (browser == null && browserError == null) {
                val result = JcefDiagnostics.createBrowser()
                browser = result.browser
                if (browser == null) {
                    browserError = result.diagnostics.joinToString("\n")
                }
            }
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
        },
        update = {                                                 }
    )
}





@Composable
private fun EmptyContent(modifier: Modifier) {
    val theme = LocalThemeColors.current
    val fs = LocalPlatformFonts.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.Bg.primary)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "_No content_",
            fontSize = fs.small,
            color = theme.Text.disabled
        )
    }
}

/**
 * Fallback when the HTML is too large for JCEF to handle comfortably.
 * Shows the first few thousand chars as plain text with a warning.
 */
@Composable
private fun OversizeFallback(html: String, modifier: Modifier) {
    val theme = LocalThemeColors.current
    val fs = LocalPlatformFonts.current
    val preview = html
        .replace(Regex("<[^>]+>"), " ")   // strip tags
        .replace(Regex("\\s+"), " ")      // collapse whitespace
        .take(2000)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(theme.Bg.primary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "⚠ Content too large (${html.length / 1024} KB). Showing plain-text preview.",
            fontSize = fs.xsmall,
            color = theme.Text.error
        )
        Spacer(Modifier.height(12.dp))
        Text(
            preview,
            fontSize = fs.small,
            color = theme.Text.primary,
            lineHeight = fs.label
        )
        if (html.replace(Regex("<[^>]+>"), "").length > 2000) {
            Spacer(Modifier.height(8.dp))
            Text(
                "… (truncated)",
                fontSize = fs.xsmall,
                color = theme.Text.disabled
            )
        }
    }
}






private fun buildThemedPage(bodyHtml: String): String {
    val css = buildThemeCss()
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                $css
            </style>
        </head>
        <body>
            $bodyHtml
        </body>
        </html>
    """.trimIndent()
}

/**
 * Builds a comprehensive CSS block that mirrors the active JetBrains theme.
 *
 * Strategy:
 *  1. CSS custom properties in :root → single source of truth for all colors
 *  2. A reset/normalize layer
 *  3. Full element coverage — anything a VCS platform might render
 *  4. Both browser-default overrides AND GitHub-style class handling
 *
 * Colors are pulled from JBColor/JBUI so they react to IDE theme changes.
 */
private fun buildThemeCss(): String {

    val bg = ColorUtil.toHtmlColor(JBColor.PanelBackground)
    val fg = ColorUtil.toHtmlColor(JBColor.foreground())
    val link = ColorUtil.toHtmlColor(JBUI.CurrentTheme.Link.Foreground.ENABLED)
    val linkHover = ColorUtil.toHtmlColor(JBUI.CurrentTheme.Link.Foreground.HOVERED)
    val border = ColorUtil.toHtmlColor(JBColor.border())
    val secondary = ColorUtil.toHtmlColor(run {

        val fgC = JBColor.foreground()
        val bgC = JBColor.PanelBackground
        blend(fgC, bgC, 0.55f)
    })
    val codeBg = if (JBColor.isBright()) "#F0F2F5" else "#2D333B"
    val codeBlockBg = if (JBColor.isBright()) "#F6F8FA" else "#1C2128"
    val surface = if (JBColor.isBright()) "#FFFFFF" else "#1C2128"
    val tableStripe = if (JBColor.isBright()) "#F6F8FA" else "#22272E"
    val quoteBorder = if (JBColor.isBright()) "#D0D7DE" else "#444C56"
    val quoteFg = if (JBColor.isBright()) "#656D76" else "#8B949E"
    val inputBg = if (JBColor.isBright()) "#FFFFFF" else "#22272E"
    val inputBorder = if (JBColor.isBright()) "#D0D7DE" else "#444C56"
    val focusRing = ColorUtil.toHtmlColor(run {

        val linkC = JBUI.CurrentTheme.Link.Foreground.ENABLED
        java.awt.Color(linkC.red, linkC.green, linkC.blue, 76)   // ~30% alpha
    })
    val shadow = if (JBColor.isBright()) "rgba(31,35,40,0.08)" else "rgba(1,4,9,0.3)"
    val markBg = if (JBColor.isBright()) "#FFF8C5" else "#5B4B00"
    val kbdBg = if (JBColor.isBright()) "#F6F8FA" else "#272B33"
    val kbdBorder = if (JBColor.isBright()) "#D0D7DE" else "#444C56"

    return """
                                                                            
           Theme CSS — mirrors active JetBrains IDE theme
           Generated at render time; reloaded on theme switch.
           ================================================================= */

                                                                             
        *, *::before, *::after { box-sizing: border-box; }

        :root {
            --bg: $bg;
            --fg: $fg;
            --link: $link;
            --link-hover: $linkHover;
            --border: $border;
            --secondary: $secondary;
            --code-bg: $codeBg;
            --code-block-bg: $codeBlockBg;
            --surface: $surface;
            --table-stripe: $tableStripe;
            --quote-border: $quoteBorder;
            --quote-fg: $quoteFg;
            --input-bg: $inputBg;
            --input-border: $inputBorder;
            --focus-ring: $focusRing;
            --shadow: $shadow;
            --mark-bg: $markBg;
            --kbd-bg: $kbdBg;
            --kbd-border: $kbdBorder;
        }

        html {
            font-size: 13px;
            -webkit-text-size-adjust: 100%;
            text-size-adjust: 100%;
        }

        body {
            margin: 0;
            padding: 12px 16px;
            background-color: var(--bg);
            color: var(--fg);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI",
                         Helvetica, Arial, sans-serif, "Apple Color Emoji",
                         "Segoe UI Emoji", "Segoe UI Symbol";
            font-size: 1rem;
            line-height: 1.6;
            word-wrap: break-word;
            overflow-wrap: break-word;
            -webkit-font-smoothing: antialiased;
        }

                                                                             
        h1, h2, h3, h4, h5, h6 {
            margin-top: 24px;
            margin-bottom: 16px;
            font-weight: 600;
            line-height: 1.25;
            color: var(--fg);
        }
        h1 { font-size: 2em; border-bottom: 1px solid var(--border); padding-bottom: .3em; }
        h2 { font-size: 1.5em; border-bottom: 1px solid var(--border); padding-bottom: .3em; }
        h3 { font-size: 1.25em; }
        h4 { font-size: 1em; }
        h5 { font-size: .875em; }
        h6 { font-size: .85em; color: var(--secondary); }

        p { margin-top: 0; margin-bottom: 10px; }

                                                                             
        a {
            color: var(--link);
            text-decoration: none;
        }
        a:hover, a:focus {
            color: var(--link-hover);
            text-decoration: underline;
            outline: none;
        }
        a:visited { color: var(--link); }

                                                                             
        code {
            font-family: "JetBrains Mono", "Fira Code", "Cascadia Code",
                         "SF Mono", Menlo, Monaco, Consolas, monospace;
            font-size: 0.9em;
            background-color: var(--code-bg);
            padding: 0.2em 0.4em;
            border-radius: 3px;
        }

        pre {
            background-color: var(--code-block-bg);
            padding: 16px;
            border-radius: 6px;
            overflow-x: auto;
            margin-top: 0;
            margin-bottom: 16px;
            line-height: 1.45;
        }
        pre code {
            background-color: transparent;
            padding: 0;
            border-radius: 0;
            font-size: 0.92em;
        }

                                                                             
        blockquote {
            margin: 0 0 16px 0;
            padding: 0 1em;
            color: var(--quote-fg);
            border-left: 4px solid var(--quote-border);
        }
        blockquote > :first-child { margin-top: 0; }
        blockquote > :last-child { margin-bottom: 0; }

                                                                             
        table {
            border-collapse: collapse;
            width: 100%;
            margin-top: 0;
            margin-bottom: 16px;
            overflow: auto;
            display: block;
        }
        th, td {
            border: 1px solid var(--border);
            padding: 6px 13px;
            text-align: left;
            vertical-align: top;
        }
        th {
            font-weight: 600;
            background-color: var(--table-stripe);
        }
        tr:nth-child(even) td { background-color: var(--table-stripe); }
        thead th { position: sticky; top: 0; z-index: 1; }

                                                                             
        ul, ol {
            margin-top: 0;
            margin-bottom: 16px;
            padding-left: 2em;
        }
        li { margin-bottom: 2px; }
        li > ul, li > ol { margin-bottom: 0; }
        dd { margin-left: 2em; margin-bottom: 8px; }

                                               
        .task-list-item {
            list-style-type: none;
            margin-left: -1.5em;
        }
        .task-list-item input[type="checkbox"] {
            margin-right: 6px;
            vertical-align: middle;
        }

                                                                             
        hr {
            border: 0;
            border-bottom: 1px solid var(--border);
            margin: 24px 0;
        }

                                                                             
        img, video, svg {
            max-width: 100%;
            height: auto !important;
            border-radius: 6px;
        }
        img { display: inline-block; vertical-align: middle; }
        figure {
            margin: 0 0 16px 0;
            text-align: center;
        }
        figcaption {
            font-size: 0.85em;
            color: var(--secondary);
            margin-top: 4px;
        }
        audio, video, canvas { max-width: 100%; display: block; }
        iframe { max-width: 100%; border: 1px solid var(--border); border-radius: 6px; }
        object, embed { max-width: 100%; }

                                                                             
        details {
            border: 1px solid var(--border);
            border-radius: 6px;
            padding: 8px 16px;
            margin-bottom: 16px;
        }
        details[open] { padding-bottom: 16px; }
        summary {
            cursor: pointer;
            font-weight: 600;
            padding: 8px 0;
            outline: none;
            color: var(--fg);
        }
        summary:hover { color: var(--link); }

                                                                             
        form { margin-bottom: 16px; }
        fieldset {
            border: 1px solid var(--border);
            border-radius: 6px;
            padding: 12px 16px;
            margin-bottom: 16px;
        }
        legend { font-weight: 600; padding: 0 6px; }

        label { font-weight: 500; display: inline-block; margin-bottom: 4px; }

        input, textarea, select, button {
            font-family: inherit;
            font-size: inherit;
            line-height: inherit;
            color: var(--fg);
        }

        input[type="text"],
        input[type="email"],
        input[type="password"],
        input[type="url"],
        input[type="number"],
        input[type="search"],
        input[type="tel"],
        input[type="date"],
        input[type="datetime-local"],
        input[type="month"],
        input[type="week"],
        input[type="time"],
        textarea,
        select {
            background-color: var(--input-bg);
            border: 1px solid var(--input-border);
            border-radius: 6px;
            padding: 5px 12px;
            outline: none;
            transition: border-color 0.15s ease, box-shadow 0.15s ease;
        }
        input:focus, textarea:focus, select:focus {
            border-color: var(--focus-ring);
            box-shadow: 0 0 0 3px $shadow;
        }
        input[type="checkbox"],
        input[type="radio"] {
            accent-color: var(--link);
            margin-right: 4px;
            vertical-align: middle;
        }

        button,
        input[type="button"],
        input[type="submit"],
        input[type="reset"] {
            background-color: var(--code-bg);
            border: 1px solid var(--input-border);
            border-radius: 6px;
            padding: 5px 16px;
            cursor: pointer;
            font-weight: 500;
            transition: background-color 0.1s ease;
        }
        button:hover,
        input[type="button"]:hover,
        input[type="submit"]:hover,
        input[type="reset"]:hover {
            background-color: var(--border);
        }

                                                                             
        strong, b { font-weight: 600; }
        em, i { font-style: italic; }
        u { text-decoration: underline; }
        s, del, strike { text-decoration: line-through; }
        ins { text-decoration: underline; }
        sup, sub { font-size: 0.75em; }
        small { font-size: 0.85em; color: var(--secondary); }
        mark { background-color: var(--mark-bg); padding: 0.1em 0.2em; border-radius: 2px; }
        abbr[title] {
            text-decoration: underline dotted;
            cursor: help;
        }
        cite { font-style: italic; color: var(--secondary); }
        dfn { font-style: italic; }
        kbd {
            background-color: var(--kbd-bg);
            border: 1px solid var(--kbd-border);
            border-radius: 3px;
            padding: 0.1em 0.4em;
            font-family: "JetBrains Mono", Menlo, Monaco, Consolas, monospace;
            font-size: 0.85em;
            box-shadow: inset 0 -1px 0 var(--kbd-border);
        }
        samp {
            font-family: "JetBrains Mono", Menlo, Monaco, Consolas, monospace;
            font-size: 0.9em;
        }

                                                                             
        tt {
            font-family: "JetBrains Mono", Menlo, Monaco, Consolas, monospace;
        }
        var {
            font-family: "JetBrains Mono", Menlo, Monaco, Consolas, monospace;
            font-style: italic;
        }

                                                                             
        ruby > rt {
            font-size: 0.6em;
            color: var(--secondary);
        }

                                                                             
        svg text { fill: currentColor; }
        svg a { fill: var(--link); }

                                                                             
        ::-webkit-scrollbar { width: 8px; height: 8px; }
        ::-webkit-scrollbar-track { background: transparent; }
        ::-webkit-scrollbar-thumb {
            background: var(--border);
            border-radius: 4px;
        }
        ::-webkit-scrollbar-thumb:hover { background: var(--secondary); }

                                                                             
        ::selection {
            background-color: $focusRing;
            color: var(--bg);
        }

                                                                             
        @media print {
            body { background: white; color: black; }
            a { color: black; text-decoration: underline; }
        }
    """.trimIndent()
}

