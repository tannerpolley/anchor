package com.itsjeel01.remotevcsmanager.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Color as AwtColor

private fun AwtColor.toComposeColor(): Color =
    Color(red / 255f, green / 255f, blue / 255f, alpha / 255f)

class BgColors {
    val primary: Color = JBColor.namedColor("Panel.background", UIUtil.getPanelBackground()).toComposeColor()
    val surface: Color = JBColor.namedColor("Table.background", UIUtil.getTableBackground()).toComposeColor()
    val card: Color = run {
        val base = UIUtil.getTableBackground()
        val r = base.red; val g = base.green; val b = base.blue
        if (JBColor.isBright())
            Color((r - 8).coerceIn(0, 255) / 255f, (g - 8).coerceIn(0, 255) / 255f, (b - 8).coerceIn(0, 255) / 255f)
        else Color((r + 8).coerceIn(0, 255) / 255f, (g + 8).coerceIn(0, 255) / 255f, (b + 8).coerceIn(0, 255) / 255f)
    }
    val input: Color = JBColor.namedColor("TextField.background", UIUtil.getTextFieldBackground()).toComposeColor()
    val hover: Color = JBColor.namedColor("Table.hover.background", UIUtil.getTableSelectionBackground(false)).toComposeColor()
    val selected: Color = JBColor.namedColor("Table.selection.background", UIUtil.getTableSelectionBackground(true)).toComposeColor()
}

class TextColors {
    val primary: Color = JBColor.namedColor("Label.foreground", UIUtil.getLabelForeground()).toComposeColor()
    val secondary: Color = JBColor.namedColor("Label.infoForeground", UIUtil.getContextHelpForeground()).toComposeColor()
    val disabled: Color = JBColor.namedColor("Label.disabledForeground", UIUtil.getLabelDisabledForeground()).toComposeColor()
    val link: Color = JBColor.namedColor("Link.foreground", JBUI.CurrentTheme.Link.Foreground.ENABLED).toComposeColor()
    val linkHover: Color = JBColor.namedColor("Link.hover.foreground", JBUI.CurrentTheme.Link.Foreground.HOVERED).toComposeColor()
    val selected: Color = JBColor.namedColor("Table.selection.foreground", UIUtil.getTableSelectionForeground(true)).toComposeColor()
    val error: Color = JBColor.namedColor("Notification.error.foreground", UIUtil.getErrorForeground()).toComposeColor()
}

class BorderColors {
    val default: Color = JBColor.namedColor("Panel.border", UIUtil.getBoundsColor()).toComposeColor()
    val focused: Color = JBColor.namedColor("Button.focusedBorderColor", UIUtil.getBoundsColor()).toComposeColor()
}

class ButtonColors {
    val background: Color = JBColor.namedColor("Button.default.startBackground", JBColor(0x448AFF, 0x448AFF)).toComposeColor()
    val foreground: Color = JBColor.namedColor("Button.default.foreground", JBColor(0xFFFFFF, 0xFFFFFF)).toComposeColor()
    val dangerBackground: Color = JBColor.namedColor("Button.danger.startBackground", JBColor(0xCF222E, 0xF85149)).toComposeColor()
    val dangerForeground: Color = JBColor.namedColor("Button.danger.foreground", JBColor(0xFFFFFF, 0xFFFFFF)).toComposeColor()
}

class GitHubColors {
    val open: Color = JBColor(0x2DA44E, 0x3FB950).toComposeColor()
    val closed: Color = JBColor(0x8957E5, 0x8957E5).toComposeColor()
    val closedPr: Color = JBColor(0xCF222E, 0xF85149).toComposeColor()
}

class ThemeColors(version: Int = 0) {
    val Bg: BgColors = BgColors()
    val Text: TextColors = TextColors()
    val Border: BorderColors = BorderColors()
    val Button: ButtonColors = ButtonColors()
    val GitHub: GitHubColors = GitHubColors()
    val divider: Color = Border.default.copy(alpha = 0.5f)
    val dividerSubtle: Color = Border.default.copy(alpha = 0.25f)
}

@Composable
fun rememberThemeColors(): ThemeColors {
    val version by IdeEvents.theme.collectAsState()
    return remember(version) { ThemeColors(version) }
}

class PlatformFonts(version: Int = 0) {
    val label: TextUnit = JBUI.Fonts.label().size.toFloat().sp
    val small: TextUnit = JBUI.Fonts.smallFont().size.toFloat().sp
    val mini: TextUnit = JBUI.Fonts.miniFont().size.toFloat().sp
    val bold: TextUnit = JBUI.Fonts.label().size.toFloat().sp
    val mono: TextUnit = JBUI.Fonts.label().size.toFloat().sp
    val title: TextUnit = (JBUI.Fonts.label().size.toFloat() * 1.15f).sp
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
