package com.itsjeel01.remotevcsmanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import javax.swing.Icon

/**
 * A clickable text label without Material button min-size constraints.
 * Use in place of TextButton to avoid 40dp+ touch-target forcing row height.
 */
@Composable
fun ClickableText(text: String, onClick: () -> Unit, fontSize: TextUnit, color: Color) {
    Text(text, fontSize = fontSize, color = color, modifier = Modifier.clickable(onClick = onClick))
}

/**
 * A clickable icon without Material IconButton 48dp min-size constraints.
 * Use in place of IconButton to avoid forcing row height to 48dp.
 */
@Composable
fun ClickableIcon(icon: Icon, onClick: () -> Unit, description: String) {
    Box(Modifier.size(16.dp).clickable(onClick = onClick)) {
        PlatformIcon(icon, contentDescription = description)
    }
}
