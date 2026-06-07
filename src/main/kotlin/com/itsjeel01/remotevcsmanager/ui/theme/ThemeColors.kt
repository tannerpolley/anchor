package com.itsjeel01.remotevcsmanager.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.Color as AwtColor
import javax.swing.UIManager

private fun AwtColor.toComposeColor(): Color =
    Color(red / 255f, green / 255f, blue / 255f, alpha / 255f)

// ── Reactive event sources ───────────────────────────────────────────────

object IdeEvents {
    private val _theme = MutableStateFlow(0)
    val theme: StateFlow<Int> = _theme.asStateFlow()

    private val _fonts = MutableStateFlow(0)
    val fonts: StateFlow<Int> = _fonts.asStateFlow()

    private val _scale = MutableStateFlow(0)
    val scale: StateFlow<Int> = _scale.asStateFlow()

    init {
        UIManager.addPropertyChangeListener { e ->
            // Fire on ALL UIManager property changes — these only happen on explicit
            // user actions (theme switch, zoom, font size, DPI scale) and are rare.
            // This guarantees we never miss a zoom/font/theme change regardless of
            // the exact property name IntelliJ fires.
            _theme.value++
            _fonts.value++
            _scale.value++
        }
    }
}

// ── Named inner classes (avoids Kotlin 2.1 anonymous object type issues) ─

class BgColors {
    val primary: Color  = UIUtil.getPanelBackground().toComposeColor()
    val surface: Color  = UIUtil.getTableBackground().toComposeColor()
    val card: Color = run {
        val base = UIUtil.getTableBackground()
        val r = base.red; val g = base.green; val b = base.blue
        if (JBColor.isBright())
            Color((r - 8).coerceIn(0, 255) / 255f, (g - 8).coerceIn(0, 255) / 255f, (b - 8).coerceIn(0, 255) / 255f)
        else Color((r + 8).coerceIn(0, 255) / 255f, (g + 8).coerceIn(0, 255) / 255f, (b + 8).coerceIn(0, 255) / 255f)
    }
    val input: Color    = UIUtil.getTextFieldBackground().toComposeColor()
    val hover: Color    = UIUtil.getTableSelectionBackground(false).toComposeColor()
    val selected: Color = UIUtil.getTableSelectionBackground(true).toComposeColor()
}

class TextColors {
    val primary: Color   = UIUtil.getLabelForeground().toComposeColor()
    val secondary: Color = UIUtil.getContextHelpForeground().toComposeColor()
    val disabled: Color  = UIUtil.getLabelDisabledForeground().toComposeColor()
    val link: Color      = JBUI.CurrentTheme.Link.Foreground.ENABLED.toComposeColor()
    val selected: Color  = UIUtil.getTableSelectionForeground(true).toComposeColor()
    val error: Color     = UIUtil.getErrorForeground().toComposeColor()
    val onAccent: Color  = Color.White
}

class BorderColors {
    val default: Color   = UIUtil.getBoundsColor().toComposeColor()
    val focused: Color   = JBColor.namedColor("Button.focusedBorderColor", UIUtil.getBoundsColor()).toComposeColor()
    val linkHover: Color = JBUI.CurrentTheme.Link.Foreground.HOVERED.toComposeColor()
}

class AccentColors {
    val blue: Color      = JBColor.namedColor("Button.focusedBorderColor", JBColor(0x448AFF, 0x448AFF)).toComposeColor()
    val green: Color     = JBColor(0x2DA44E, 0x3FB950).toComposeColor()
    val purple: Color    = JBColor(0x8957E5, 0x8957E5).toComposeColor()
    val red: Color       = JBColor(0xCF222E, 0xF85149).toComposeColor()
    val warning: Color   = JBUI.CurrentTheme.NotificationInfo.borderColor().toComposeColor()
}

class ThemeColors(version: Int = 0) {
    val Bg: BgColors          = BgColors()
    val Text: TextColors      = TextColors()
    val Border: BorderColors  = BorderColors()
    val Accent: AccentColors  = AccentColors()
    val divider: Color        = UIUtil.getBoundsColor().toComposeColor().copy(alpha = 0.5f)
    val dividerSubtle: Color  = UIUtil.getBoundsColor().toComposeColor().copy(alpha = 0.25f)
}

@Composable
fun rememberThemeColors(): ThemeColors {
    val version by IdeEvents.theme.collectAsState()
    return remember(version) { ThemeColors(version) }
}

class PlatformFonts(version: Int = 0) {
    val label: TextUnit  = JBUI.Fonts.label().size.toFloat().sp
    val small: TextUnit  = JBUI.Fonts.smallFont().size.toFloat().sp
    val mono: TextUnit   = JBUI.Fonts.label().size.toFloat().sp
    val title: TextUnit  = (JBUI.Fonts.label().size.toFloat() * 1.15f).sp
    val xsmall: TextUnit = (JBUI.Fonts.smallFont().size.toFloat() * 0.9f).sp
}

@Composable
fun rememberPlatformFonts(): PlatformFonts {
    val fontV by IdeEvents.fonts.collectAsState()
    val scaleV by IdeEvents.scale.collectAsState()
    return remember(fontV, scaleV) { PlatformFonts(fontV + scaleV) }
}

val LocalThemeColors = staticCompositionLocalOf { ThemeColors(0) }
val LocalPlatformFonts = staticCompositionLocalOf { PlatformFonts(0) }
