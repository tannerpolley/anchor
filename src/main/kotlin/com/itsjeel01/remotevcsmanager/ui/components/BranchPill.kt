package com.itsjeel01.remotevcsmanager.ui.components

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itsjeel01.remotevcsmanager.ui.theme.LocalPlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.LocalThemeColors

/**
 * Inline pill component for branch names — Compose.
 * Uses platform-derived font sizes and theme-aware colors.
 */
@Composable
fun BranchPill(branchName: String, modifier: Modifier = Modifier) {
    val theme = LocalThemeColors.current
    val fs = LocalPlatformFonts.current

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = theme.Bg.card
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⑂", fontSize = fs.xsmall, color = theme.Text.secondary)
            Spacer(Modifier.width(4.dp))

            Text(
                text = branchName,
                fontSize = fs.mono,
                fontFamily = FontFamily.Monospace,
                color = theme.Text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}