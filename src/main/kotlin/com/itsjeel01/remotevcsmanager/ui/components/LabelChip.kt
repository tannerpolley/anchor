package com.itsjeel01.remotevcsmanager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
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
        substring(0, 2).toInt(16),
        substring(2, 4).toInt(16),
        substring(4, 6).toInt(16)
    )
} catch (_: Exception) { Color.Gray }

private fun Color.textColor(): Color {
    val luminance = (red * 299 + green * 587 + blue * 114) / 1000
    return if (luminance > 140) Color.Black else Color.White
}

@OptIn(ExperimentalMaterialApi::class)
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

    Surface(
        onClick = { onToggle?.invoke() },
        shape = RoundedCornerShape(4.dp),
        color = chipColor,
        border = if (selected) BorderStroke(2.dp, theme.Border.focused) else null,
        modifier = modifier
    ) {
        Text(
            label.name,
            color = textColor,
            fontSize = fs.xsmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
