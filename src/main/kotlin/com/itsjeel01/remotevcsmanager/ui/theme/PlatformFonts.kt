package com.itsjeel01.remotevcsmanager.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.intellij.util.ui.JBUI

/**
 * Platform-derived font sizes that respect the IDE's font settings and display scale.
 *
 * `JBUI.Fonts.label().size` returns the correct point size (e.g., 13) already scaled
 * for the user's font preferences. We use it directly as Compose `sp` so that
 * Compose's own density scaling handles Retina/high-DPI correctly.
 *
 * Usage:
 * ```kotlin
 * val fs = PlatformFonts.current()
 * Text("Hello", fontSize = fs.label)
 * Text("metadata", fontSize = fs.small)
 * ```
 */
object PlatformFonts {

    @Composable
    fun current(): FontScale = FontScale()

    class FontScale {
        /** Primary UI text — matches IDE label font size (e.g., 13sp) */
        val label: TextUnit  get() = JBUI.Fonts.label().size.toFloat().sp
        /** Secondary / metadata text (e.g., 11sp) */
        val small: TextUnit  get() = JBUI.Fonts.smallFont().size.toFloat().sp
        /** Monospace text (same as label; callers apply fontFamily) */
        val mono: TextUnit   get() = JBUI.Fonts.label().size.toFloat().sp
        /** Slightly larger for detail panel titles (~15% bigger) */
        val title: TextUnit  get() = (JBUI.Fonts.label().size.toFloat() * 1.15f).sp
        /** Extra-small for badges, chip labels */
        val xsmall: TextUnit get() = (JBUI.Fonts.smallFont().size.toFloat() * 0.9f).sp
    }
}
