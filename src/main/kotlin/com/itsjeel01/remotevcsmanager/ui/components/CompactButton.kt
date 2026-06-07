package com.itsjeel01.remotevcsmanager.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.itsjeel01.remotevcsmanager.ui.theme.LocalPlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.LocalThemeColors

@Composable
fun CompactButton(
    text: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    textColor: Color = LocalThemeColors.current.Text.onAccent,
    fontSize: androidx.compose.ui.unit.TextUnit = LocalPlatformFonts.current.small
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = textColor
        ),
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        shape = RoundedCornerShape(2.dp),
        modifier = modifier.height(30.dp)
    ) {
        Text(text, fontSize = fontSize)
    }
}
