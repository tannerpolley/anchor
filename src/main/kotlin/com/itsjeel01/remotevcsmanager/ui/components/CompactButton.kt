package com.itsjeel01.remotevcsmanager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itsjeel01.remotevcsmanager.ui.theme.LocalPlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.LocalThemeColors

enum class ButtonVariant { Primary, Secondary, Danger }

@Composable
fun CompactButton(
    text: String,
    onClick: () -> Unit,
    variant: ButtonVariant = ButtonVariant.Primary,
    modifier: Modifier = Modifier
) {
    val theme = LocalThemeColors.current
    val fs = LocalPlatformFonts.current

    val (backgroundColor, textColor, border) = when (variant) {
        ButtonVariant.Primary -> Triple(theme.Button.background, theme.Button.foreground, null)
        ButtonVariant.Danger -> Triple(theme.Button.dangerBackground, theme.Button.dangerForeground, null)
        ButtonVariant.Secondary -> Triple(
            androidx.compose.ui.graphics.Color.Transparent,
            theme.Text.primary,
            BorderStroke(1.dp, theme.Border.default.copy(alpha = 0.5f))
        )
    }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = textColor
        ),
        border = border,
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        shape = RoundedCornerShape(2.dp),
        modifier = modifier.height(30.dp)
    ) {
        Text(text, fontSize = fs.small)
    }
}
