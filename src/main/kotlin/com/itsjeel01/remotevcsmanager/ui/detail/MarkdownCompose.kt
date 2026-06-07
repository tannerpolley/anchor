package com.itsjeel01.remotevcsmanager.ui.detail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.intellij.ide.BrowserUtil
import com.itsjeel01.remotevcsmanager.ui.theme.LocalPlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.LocalThemeColors
import com.itsjeel01.remotevcsmanager.ui.theme.ThemeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.skia.Image as SkiaImage

// ── Block types ───────────────────────────────────────────────────────────

private enum class BlockType { PARAGRAPH, H1, H2, H3, UL, OL, CODE, QUOTE, HR }

private sealed class Fragment {
    data class Text(val annotated: AnnotatedString) : Fragment()
    data class Image(val url: String, val alt: String) : Fragment()
}

// ── Image cache (LRU memory cache + OkHttp disk cache) ────────────────────

private const val MAX_CACHED_IMAGES = 100

/** In-memory LRU cache for decoded ImageBitmaps — avoids re-download + re-decode */
private val imageBitmapCache = object : LinkedHashMap<String, ImageBitmap>(MAX_CACHED_IMAGES, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>): Boolean = size > MAX_CACHED_IMAGES
}

/** OkHttp client with connection pooling, reduced timeouts, and disk cache */
private val imageClient = OkHttpClient.Builder()
    .followRedirects(true).followSslRedirects(true)
    .connectionPool(okhttp3.ConnectionPool(10, 30, java.util.concurrent.TimeUnit.SECONDS))
    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
    .writeTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .cache(okhttp3.Cache(
        java.io.File(System.getProperty("java.io.tmpdir"), "remotevcs-vcs-cache").also { it.mkdirs() },
        20L * 1024 * 1024
    ))
    .build()

@Composable
private fun rememberImageBitmap(url: String): ImageBitmap? {
    // Check memory cache first (instant — no I/O, no decode)
    synchronized(imageBitmapCache) {
        imageBitmapCache[url]?.let { return it }
    }

    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = null
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(url).header("User-Agent", "RemoteVcsManager").build()
                val resp = imageClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    resp.body?.bytes()?.let { bytes ->
                        val skiaImage = SkiaImage.makeFromEncoded(bytes)
                        if (skiaImage != null) {
                            val bmp = skiaImage.toComposeImageBitmap()
                            synchronized(imageBitmapCache) { imageBitmapCache[url] = bmp }
                            bitmap = bmp
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }
    return bitmap
}

// Conversion uses SkiaImage.makeFromEncoded + toComposeImageBitmap (see above)

// ── Main entry point ──────────────────────────────────────────────────────

object MarkdownCompose {

    @Composable
    fun Block(markdown: String, modifier: Modifier = Modifier) {
        val theme = LocalThemeColors.current
        val fs = LocalPlatformFonts.current
        if (markdown.isBlank()) return

        Column(modifier = modifier) {
            val lines = markdown.split("\n")
            var i = 0
            while (i < lines.size) {
                val trimmed = lines[i].trim()
                when {
                    trimmed.startsWith("```") -> i = codeBlock(lines, i)
                    trimmed.startsWith("### ") -> inlineBlock(trimmed.removePrefix("### "), BlockType.H3)
                    trimmed.startsWith("## ") -> inlineBlock(trimmed.removePrefix("## "), BlockType.H2)
                    trimmed.startsWith("# ") -> inlineBlock(trimmed.removePrefix("# "), BlockType.H1)
                    trimmed.startsWith("> ") -> inlineBlock(trimmed.removePrefix("> "), BlockType.QUOTE)
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> i = listBlock(lines, i, ordered = false)
                    Regex("^\\d+\\.\\s").containsMatchIn(trimmed) -> i = listBlock(lines, i, ordered = true)
                    trimmed == "---" || trimmed == "***" -> HorizontalRule()
                    trimmed.isBlank() -> Spacer(Modifier.height(4.dp))
                    else -> inlineBlock(trimmed, BlockType.PARAGRAPH)
                }
                i++
            }
        }
    }

    // ── Block renderers ───────────────────────────────────────────────────

    @Composable
    private fun codeBlock(lines: List<String>, start: Int): Int {
        val theme = LocalThemeColors.current
        val fs = LocalPlatformFonts.current
        val code = mutableListOf<String>()
        var i = start + 1
        while (i < lines.size && !lines[i].trim().startsWith("```")) { code.add(lines[i]); i++ }
        Surface(Modifier.fillMaxWidth().padding(horizontal = 0.dp, vertical = 3.dp),
            shape = RoundedCornerShape(4.dp), color = theme.Bg.surface) {
            Text(code.joinToString("\n"), fontFamily = FontFamily.Monospace, fontSize = fs.mono,
                color = theme.Text.primary.copy(alpha = 0.9f), modifier = Modifier.padding(8.dp))
        }
        return i
    }

    @Composable
    private fun listBlock(lines: List<String>, start: Int, ordered: Boolean): Int {
        val theme = LocalThemeColors.current
        val fs = LocalPlatformFonts.current
        val items = mutableListOf<String>()
        var j = start
        while (j < lines.size) {
            val t = lines[j].trim()
            if ((ordered && Regex("^\\d+\\.\\s").containsMatchIn(t)) ||
                (!ordered && (t.startsWith("- ") || t.startsWith("* ")))) {
                items.add(t.replaceFirst(Regex("^\\d+\\.\\s|^- |^\\* "), "")); j++
            } else if (t.isBlank()) break else break
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 1.dp)) {
            items.forEachIndexed { idx, item ->
                Row(Modifier.padding(horizontal = 0.dp, vertical = 1.dp)) {
                    Text(if (ordered) "${idx + 1}." else "•", color = theme.Text.secondary,
                        fontSize = fs.small, modifier = Modifier.width(20.dp))
                    inlineContent(item, size = fs.label, color = theme.Text.primary)
                }
            }
        }
        return j - 1
    }

    @Composable
    private fun HorizontalRule() {
        val theme = LocalThemeColors.current
        val fs = LocalPlatformFonts.current
        Spacer(Modifier.height(4.dp))
        Divider(color = theme.dividerSubtle, thickness = 0.5.dp)
    }

    // ── Inline renderer (text + images) ───────────────────────────────────

    @Composable
    private fun inlineBlock(text: String, type: BlockType) {
        val theme = LocalThemeColors.current
        val fs = LocalPlatformFonts.current
        val size = when (type) { BlockType.H1 -> fs.title; BlockType.H2 -> fs.label; BlockType.H3 -> fs.small; BlockType.QUOTE -> fs.small; else -> fs.label }
        val color = if (type == BlockType.QUOTE) theme.Text.secondary else theme.Text.primary
        val bg = if (type == BlockType.QUOTE) theme.Bg.hover else androidx.compose.ui.graphics.Color.Transparent
        Surface(Modifier.fillMaxWidth().padding(horizontal = 0.dp, vertical = if (type == BlockType.H1 || type == BlockType.H2) 3.dp else 1.dp),
            shape = if (type == BlockType.QUOTE) RoundedCornerShape(4.dp) else RoundedCornerShape(0.dp), color = bg) {
            inlineContent(text, size = size, color = color,
                fontWeight = if (type in listOf(BlockType.H1, BlockType.H2, BlockType.H3)) FontWeight.Bold else FontWeight.Normal,
                startPadding = if (type == BlockType.QUOTE) 8.dp else 0.dp)
        }
    }

    @Composable
    private fun inlineContent(
        text: String,
        size: TextUnit,
        color: androidx.compose.ui.graphics.Color,
        fontWeight: FontWeight = FontWeight.Normal,
        startPadding: Dp = 0.dp
    ) {
        val theme = LocalThemeColors.current
        val fs = LocalPlatformFonts.current
        val fragments = parseFragments(text, theme)
        if (fragments.isEmpty()) return

        if (fragments.all { it is Fragment.Text }) {
            val combined = buildAnnotatedString { fragments.forEach { if (it is Fragment.Text) append(it.annotated) } }
            ClickableText(combined, onClick = { onUriClick(combined, it) },
                style = LocalTextStyle.current.copy(fontSize = size, fontWeight = fontWeight, color = color),
                modifier = Modifier.padding(start = startPadding))
        } else {
            Column(Modifier.padding(start = startPadding)) {
                var buf = AnnotatedString.Builder()
                fragments.forEach { frag ->
                    when (frag) {
                        is Fragment.Text -> buf.append(frag.annotated)
                        is Fragment.Image -> {
                            if (buf.length > 0) {
                                val flush = buf.toAnnotatedString()
                                ClickableText(flush, onClick = { onUriClick(flush, it) },
                                    style = LocalTextStyle.current.copy(fontSize = size, fontWeight = fontWeight, color = color))
                                buf = AnnotatedString.Builder()
                            }
                            inlineImage(frag.url, frag.alt)
                        }
                    }
                }
                if (buf.length > 0) {
                    val flush = buf.toAnnotatedString()
                    ClickableText(flush, onClick = { onUriClick(flush, it) },
                        style = LocalTextStyle.current.copy(fontSize = size, fontWeight = fontWeight, color = color))
                }
            }
        }
    }

    @Composable
    private fun inlineImage(url: String, alt: String) {
        val theme = LocalThemeColors.current
        val fs = LocalPlatformFonts.current
        val bitmap = rememberImageBitmap(url)
        if (bitmap != null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(bitmap = bitmap, contentDescription = alt,
                    modifier = Modifier.widthIn(max = 560.dp).heightIn(max = 400.dp).fillMaxWidth(0.85f).padding(vertical = 4.dp),
                    contentScale = ContentScale.Fit)
            }
        } else {
            Text("🖼 $alt", fontSize = fs.xsmall, color = theme.Text.disabled,
                modifier = Modifier.padding(vertical = 4.dp))
        }
    }

    // ── Fragment parser ───────────────────────────────────────────────────

    private fun parseFragments(text: String, theme: ThemeColors): List<Fragment> {
        val result = mutableListOf<Fragment>()
        var remaining = text
        while (remaining.isNotEmpty()) {
            val imgHtml = Regex("""<img\b[^>]*?src\s*=\s*["']([^"']+)["'][^>]*?/?>""", RegexOption.IGNORE_CASE).find(remaining)
            val image = Regex("""!\[([^\]]*)\]\(([^)]+)\)""").find(remaining)
            val bold = Regex("""\*\*(.+?)\*\*""").find(remaining)
            val bold2 = Regex("""__(.+?)__""").find(remaining)
            val link = Regex("""\[([^\]]+)\]\(([^)]+)\)""").find(remaining)
            val code = Regex("""`(.+?)`""").find(remaining)
            val italic = Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)""").find(remaining)
            val italic2 = Regex("""(?<!_)_(?!_)(.+?)(?<!_)_(?!_)""").find(remaining)
            val strike = Regex("""~~(.+?)~~""").find(remaining)
            val candidates = listOfNotNull(imgHtml, image, bold, bold2, link, code, italic, italic2, strike)
            if (candidates.isEmpty()) { result.add(Fragment.Text(parseInline(remaining, theme))); break }
            val first = candidates.minByOrNull { it.range.first }!!
            if (first.range.first > 0) result.add(Fragment.Text(parseInline(remaining.substring(0, first.range.first), theme)))
            when {
                first == imgHtml || first == image -> {
                    val url = if (first == imgHtml) first.groupValues[1] else first.groupValues[2]
                    val alt = if (first == imgHtml) {
                        Regex("""alt\s*=\s*["']([^"']+)["']""").find(first.value)?.groupValues?.get(1) ?: "image"
                    } else first.groupValues[1].ifBlank { "image" }
                    result.add(Fragment.Image(url, alt))
                }
                first == link -> result.add(Fragment.Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = theme.Text.link, textDecoration = TextDecoration.Underline)) {
                        pushStringAnnotation(URL_ANNOTATION, first.groupValues[2]); append(first.groupValues[1]); pop()
                    }
                }))
                first == bold || first == bold2 -> result.add(Fragment.Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(first.groupValues[1]) }
                }))
                first == code -> result.add(Fragment.Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = theme.Bg.surface, color = theme.Accent.blue)) { append(first.groupValues[1]) }
                }))
                first == italic || first == italic2 -> result.add(Fragment.Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(first.groupValues[1]) }
                }))
                first == strike -> result.add(Fragment.Text(buildAnnotatedString {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(first.groupValues[1]) }
                }))
            }
            remaining = remaining.substring(first.range.last + 1)
        }
        return result
    }

    private fun parseInline(text: String, theme: ThemeColors): AnnotatedString = buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            val bold = Regex("""\*\*(.+?)\*\*""").find(remaining)
            val bold2 = Regex("""__(.+?)__""").find(remaining)
            val link = Regex("""\[([^\]]+)\]\(([^)]+)\)""").find(remaining)
            val code = Regex("""`(.+?)`""").find(remaining)
            val italic = Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)""").find(remaining)
            val italic2 = Regex("""(?<!_)_(?!_)(.+?)(?<!_)_(?!_)""").find(remaining)
            val strike = Regex("""~~(.+?)~~""").find(remaining)
            val candidates = listOfNotNull(bold, bold2, link, code, italic, italic2, strike)
            if (candidates.isEmpty()) { append(remaining); break }
            val first = candidates.minByOrNull { it.range.first }!!
            append(remaining.substring(0, first.range.first))
            when {
                first == link -> withStyle(SpanStyle(color = theme.Text.link, textDecoration = TextDecoration.Underline)) {
                    pushStringAnnotation(URL_ANNOTATION, first.groupValues[2]); append(first.groupValues[1]); pop()
                }
                first == bold || first == bold2 -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(first.groupValues[1]) }
                first == code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = theme.Bg.surface, color = theme.Accent.blue)) { append(first.groupValues[1]) }
                first == italic || first == italic2 -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(first.groupValues[1]) }
                first == strike -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(first.groupValues[1]) }
            }
            remaining = remaining.substring(first.range.last + 1)
        }
    }

    private fun onUriClick(annotated: AnnotatedString, offset: Int) {
        annotated.getStringAnnotations(URL_ANNOTATION, offset, offset).firstOrNull()?.let {
            BrowserUtil.browse(it.item)
        }
    }

    private const val URL_ANNOTATION = "url"
}
