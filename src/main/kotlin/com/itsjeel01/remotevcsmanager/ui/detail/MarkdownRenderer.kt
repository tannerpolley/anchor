package com.itsjeel01.remotevcsmanager.ui.detail

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI

object MarkdownRenderer {

    fun render(md: String, baseFontSize: Int = JBUI.Fonts.label().size): String {
        try {
            val lines = md.split("\n")
            val sb = StringBuilder()
            var inCodeBlock = false
            var inList = false

            for (line in lines) {
                if (line.trimStart().startsWith("```")) {
                    if (inCodeBlock) {
                        sb.append("</code></pre>\n"); inCodeBlock = false
                    } else {
                        sb.append("<pre style='background:#${if (JBColor.isBright()) "f0f0f0" else "2d2d2d"};padding:8px;border-radius:4px;overflow-x:auto'><code>")
                        inCodeBlock = true
                    }
                    continue
                }
                if (inCodeBlock) { sb.append(escapeHtml(line)).append("\n"); continue }

                var processed = inlineMarkdown(line)

                when {
                    processed.startsWith("### ") -> sb.append("<h4>").append(inlineMarkdown(processed.removePrefix("### ").trim())).append("</h4>\n")
                    processed.startsWith("## ") -> sb.append("<h3>").append(inlineMarkdown(processed.removePrefix("## ").trim())).append("</h3>\n")
                    processed.startsWith("# ") -> sb.append("<h2>").append(inlineMarkdown(processed.removePrefix("# ").trim())).append("</h2>\n")
                    processed.trimStart().startsWith("- ") || processed.trimStart().startsWith("* ") -> {
                        if (!inList) { sb.append("<ul>"); inList = true }
                        val content = processed.trimStart().removePrefix("- ").removePrefix("* ")
                        sb.append("<li>").append(inlineMarkdown(content)).append("</li>\n")
                    }
                    Regex("^\\d+\\.\\s").containsMatchIn(processed.trimStart()) -> {
                        if (!inList) { sb.append("<ol>"); inList = true }
                        val content = processed.trimStart().replaceFirst(Regex("^\\d+\\.\\s"), "")
                        sb.append("<li>").append(inlineMarkdown(content)).append("</li>\n")
                    }
                    processed.trim() == "---" || processed.trim() == "***" -> {
                        if (inList) { sb.append(closeList(sb)); inList = false }
                        sb.append("<hr>\n")
                    }
                    processed.isBlank() -> {
                        if (inList) { sb.append(closeList(sb)); inList = false }
                        sb.append("<br>\n")
                    }
                    else -> {
                        if (inList) { sb.append(closeList(sb)); inList = false }
                        sb.append("<p>").append(processed).append("</p>\n")
                    }
                }
            }
            if (inList) { sb.append(closeList(sb)) }
            return sb.toString()
        } catch (e: Exception) {
            return "<p>${md.replace("<", "&lt;").replace(">", "&gt;")}</p>"
        }
    }

    fun pageCss(baseFontSize: Int): String {
        return try {
            val monoSize = (baseFontSize * 0.92).toInt()
            "font-size:${baseFontSize}pt;line-height:1.5;" +
               "word-wrap:break-word;overflow-wrap:break-word;" +
               "color:${if (JBColor.isBright()) "#1f1f1f" else "#d4d4d4"};" +
               "margin:0;padding:0;" +
               "img{max-width:100%;}" +
               "pre,code{font-family:JetBrains Mono,Fira Code,monospace;font-size:${monoSize}pt;}" +
               "pre{overflow-x:auto;}" +
               "a{color:${if (JBColor.isBright()) "#0969da" else "#58a6ff"};}" +
               "h2,h3,h4{margin:8px 0 4px 0;font-weight:600;}" +
               "p{margin:2px 0;}" +
               "ul,ol{margin:2px 0;padding-left:20px;}" +
               "li{margin:1px 0;}" +
               "hr{border:none;border-top:1px solid ${if (JBColor.isBright()) "#d0d7de" else "#30363d"};margin:8px 0;}"
        } catch (e: Exception) {
            "font-size:${baseFontSize}pt;line-height:1.5;word-wrap:break-word;"
        }
    }

    private fun inlineMarkdown(text: String): String {
        var result = escapeHtml(text)
        result = Regex("\\*\\*(.+?)\\*\\*").replace(result) { "<b>${it.groupValues[1]}</b>" }
        result = Regex("__(.+?)__").replace(result) { "<b>${it.groupValues[1]}</b>" }
        result = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)").replace(result) { "<i>${it.groupValues[1]}</i>" }
        result = Regex("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)").replace(result) { "<i>${it.groupValues[1]}</i>" }
        result = Regex("`(.+?)`").replace(result) { "<code style='background:#${if (JBColor.isBright()) "eee" else "3d3d3d"};padding:1px 4px;border-radius:3px'>${it.groupValues[1]}</code>" }
        result = Regex("\\[(.+?)\\]\\((.+?)\\)").replace(result) { "<a href='${it.groupValues[2]}'>${it.groupValues[1]}</a>" }
        result = Regex("~~(.+?)~~").replace(result) { "<s>${it.groupValues[1]}</s>" }
        return result
    }

    private fun closeList(sb: StringBuilder) = if (sb.toString().contains("<ul>")) "</ul>\n" else "</ol>\n"

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
