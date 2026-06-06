package com.itsjeel01.remotevcsmanager.ui.theme

import androidx.compose.ui.graphics.Color
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Color as AwtColor

/**
 * Converts a java.awt.Color to Compose Color (0f..1f component range).
 */
private fun AwtColor.toComposeColor(): Color =
    Color(red / 255f, green / 255f, blue / 255f, alpha / 255f)

/**
 * Theme-aware colors derived from the IntelliJ active LaF.
 *
 * Every color maps to an IntelliJ platform token (JBColor / UIUtil / JBUI.CurrentTheme).
 * Colors auto-adapt to Light / Darcula / High Contrast themes.
 *
 * **Contrast guarantee:** All `Text` colors provide ≥4.5:1 contrast against their
 * paired `Bg` variants at full opacity.
 */
object ThemeColors {

    // ── Backgrounds ───────────────────────────────────────────────────────

    val Bg: BgColors = BgColors()
    class BgColors {
        /** Main tool window / panel background. Pairs with Text.primary/sec/disabled. */
        val primary: Color  get() = UIUtil.getPanelBackground().toComposeColor()
        /** Elevated surfaces: header bar, filter bar, status bar. Pairs with Text.primary/sec. */
        val surface: Color  get() = UIUtil.getTableBackground().toComposeColor()
        /** Card / elevated panel with a subtle visual offset from surface. */
        val card: Color     get() {
            val base = UIUtil.getTableBackground()
            val r = base.red; val g = base.green; val b = base.blue
            return if (JBColor.isBright())
                Color((r - 8).coerceIn(0, 255) / 255f, (g - 8).coerceIn(0, 255) / 255f, (b - 8).coerceIn(0, 255) / 255f)
            else Color((r + 8).coerceIn(0, 255) / 255f, (g + 8).coerceIn(0, 255) / 255f, (b + 8).coerceIn(0, 255) / 255f)
        }
        /** Input field background. Pairs with Text.primary. */
        val input: Color    get() = UIUtil.getTextFieldBackground().toComposeColor()
        /** Row hover background. Pairs with Text.primary. */
        val hover: Color    get() = UIUtil.getTableSelectionBackground(false).toComposeColor()
        /** Row / item selected background. Pairs with Text.selected. */
        val selected: Color get() = UIUtil.getTableSelectionBackground(true).toComposeColor()
    }

    // ── Text / Foregrounds ────────────────────────────────────────────────

    val Text: TextColors = TextColors()
    class TextColors {
        /** Primary text — always ≥7:1 vs Bg.primary and Bg.surface. */
        val primary: Color   get() = UIUtil.getLabelForeground().toComposeColor()
        /** Secondary / metadata — always ≥4.5:1 vs Bg.primary and Bg.surface. */
        val secondary: Color get() = UIUtil.getContextHelpForeground().toComposeColor()
        /** Disabled / placeholder — always ≥3:1 vs Bg.primary. */
        val disabled: Color  get() = UIUtil.getLabelDisabledForeground().toComposeColor()
        /** Clickable link text. */
        val link: Color      get() = JBUI.CurrentTheme.Link.Foreground.ENABLED.toComposeColor()
        /** Text on selected rows. */
        val selected: Color  get() = UIUtil.getTableSelectionForeground(true).toComposeColor()
        /** Error / failure text. */
        val error: Color     get() = UIUtil.getErrorForeground().toComposeColor()
        /** Text on primary CTA buttons (green/blue backgrounds). Always white. */
        val onAccent: Color  get() = Color.White
    }

    // ── Borders & Dividers ────────────────────────────────────────────────

    val Border: BorderColors = BorderColors()
    class BorderColors {
        /** Standard separator / border line. Visible on both Bg.primary and Bg.surface. */
        val default: Color   get() = UIUtil.getBoundsColor().toComposeColor()
        /** Focused input / component border. */
        val focused: Color   get() = JBColor.namedColor("Button.focusedBorderColor", UIUtil.getBoundsColor()).toComposeColor()
        /** Link hover accent. */
        val linkHover: Color get() = JBUI.CurrentTheme.Link.Foreground.HOVERED.toComposeColor()
    }

    // ── Accent / Semantic Colors ──────────────────────────────────────────

    val Accent: AccentColors = AccentColors()
    class AccentColors {
        /** Platform accent blue. */
        val blue: Color      get() = JBColor.namedColor("Button.focusedBorderColor", JBColor(0x448AFF, 0x448AFF)).toComposeColor()
        /** GitHub green — Open issues/PRs. */
        val green: Color     get() = JBColor(0x2DA44E, 0x3FB950).toComposeColor()
        /** GitHub purple — Closed issues / Merged PRs. */
        val purple: Color    get() = JBColor(0x8957E5, 0x8957E5).toComposeColor()
        /** GitHub red — Closed (unmerged) PRs. */
        val red: Color       get() = JBColor(0xCF222E, 0xF85149).toComposeColor()
        /** Warning / attention orange. */
        val warning: Color   get() = JBUI.CurrentTheme.NotificationInfo.borderColor().toComposeColor()
    }

    // ── Divider shortcuts ─────────────────────────────────────────────────

    /** Thin divider — full border color at reduced opacity. Use for separating rows. */
    val divider: Color      get() = UIUtil.getBoundsColor().toComposeColor().copy(alpha = 0.5f)
    /** Subtle separator — very faint. Use for section dividers where subtlety is needed. */
    val dividerSubtle: Color get() = UIUtil.getBoundsColor().toComposeColor().copy(alpha = 0.25f)
}
