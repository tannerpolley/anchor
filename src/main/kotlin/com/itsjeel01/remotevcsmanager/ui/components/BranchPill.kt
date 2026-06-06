package com.itsjeel01.remotevcsmanager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.itsjeel01.remotevcsmanager.ui.theme.PlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.ThemeColors

/**
 * Inline pill component for branch names — Compose.
 * Uses platform-derived font sizes and theme-aware colors.
 */
@Composable
fun BranchPill(branchName: String, modifier: Modifier = Modifier) {
    val fs = PlatformFonts.current()
    Surface(modifier = modifier, shape = RoundedCornerShape(3.dp), color = ThemeColors.Bg.surface,
        border = BorderStroke(1.dp, ThemeColors.Border.default.copy(alpha = 0.35f))) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⑂", fontSize = fs.xsmall, color = ThemeColors.Text.secondary)
            Spacer(Modifier.width(2.dp))
            Text(truncateName(branchName, 30), fontSize = fs.mono, fontFamily = FontFamily.Monospace, color = ThemeColors.Text.secondary)
        }
    }
}

private fun truncateName(name: String, maxLen: Int): String =
    if (name.length > maxLen) name.take(maxLen - 3) + "..." else name
