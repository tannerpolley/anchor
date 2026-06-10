package com.itsjeel01.remotevcsmanager.ui.components

/**
 * Absolute-minimum security sanitizer for user-generated VCS HTML.
 *
 * ONLY prevents JavaScript execution. Strips nothing visual.
 *
 * Three things removed:
 *   1. <script> tags  —  direct JS execution
 *   2. on* handlers   —  event-driven JS execution (onclick, onerror, etc.)
 *   3. javascript: / data:text/html URLs  —  JS URIs in links/media
 *
 * Everything else survives — <iframe>, <object>, <embed>, <style>,
 * <form>, <input>, <canvas>, <svg>, <meta>, <base>, @import, etc.
 * If a VCS platform renders it, we render it.
 */
object HtmlSanitizer {

    /**
     * Strip JS-execution vectors only. Returns structurally identical HTML
     * minus executable code.
     */
    fun sanitize(html: String): String {
        if (html.isBlank()) return html

        var result = html


        result = Regex(
            """<script\b[^>]*>.*?</script>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        ).replace(result, "")



        result = Regex(
            """\s+on\w+\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>/]+)""",
            RegexOption.IGNORE_CASE
        ).replace(result, "")



        result = Regex(
            """(href|src|action|formaction)\s*=\s*["']\s*(javascript|data\s*:\s*text/html)\s*:[^"']*["']""",
            setOf(RegexOption.IGNORE_CASE)
        ).replace(result) { mr ->
            "${mr.groupValues[1]}=\"\""
        }

        return result
    }

    /**
     * If input is a full HTML document, extract the <body> content.
     * Otherwise return the input unchanged.
     */
    fun extractBody(html: String): String {
        val trimmed = html.trimStart()

        if (trimmed.startsWith("<!DOCTYPE", ignoreCase = true) ||
            trimmed.startsWith("<html", ignoreCase = true)) {

            val bodyOpen = Regex("""<body\b[^>]*>""", RegexOption.IGNORE_CASE).find(html)
            val bodyClose = Regex("""</body\s*>""", RegexOption.IGNORE_CASE).find(html)

            if (bodyOpen != null) {
                val start = bodyOpen.range.last + 1
                val end = bodyClose?.range?.first ?: html.length
                return html.substring(start, end).trim()
            }
        }

        return html
    }
}
