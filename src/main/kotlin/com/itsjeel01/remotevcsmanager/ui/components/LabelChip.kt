package com.itsjeel01.remotevcsmanager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.itsjeel01.remotevcsmanager.models.Label
import com.itsjeel01.remotevcsmanager.ui.theme.LocalPlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.LocalThemeColors

fun String.hexToColor(): Color = try {
    Color(
        substring(0, 2).toInt(16) / 255f,
        substring(2, 4).toInt(16) / 255f,
        substring(4, 6).toInt(16) / 255f
    )
} catch (_: Exception) { Color.Gray }

private fun Color.textColor(): Color {
    val luminance = (red * 299f + green * 587f + blue * 114f) / 1000f
    return if (luminance > 0.5f) Color.Black else Color.White
}

@Composable
fun LabelChip(
    label: Label,
    selected: Boolean = false,
    onToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val fs = LocalPlatformFonts.current
    val theme = LocalThemeColors.current
    val chipColor = label.color.hexToColor()
    val textColor = chipColor.textColor()
    val borderColor = if (theme.Text.primary.luminance() > 0.5f) Color.White else Color.Black

    val surfaceModifier = if (onToggle != null) {
        modifier.clickable { onToggle() }
    } else {
        modifier
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (selected) chipColor else chipColor.copy(alpha = 0.65f),
        border = if (selected) BorderStroke(1.5.dp, borderColor) else BorderStroke(0.5.dp, chipColor.copy(alpha = 0.3f)),
        modifier = surfaceModifier
    ) {
        Text(
            if (selected) "✓ ${label.name}" else label.name,
            color = textColor,
            fontSize = fs.xsmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun Color.luminance(): Float =
    (red * 299f + green * 587f + blue * 114f) / 1000f