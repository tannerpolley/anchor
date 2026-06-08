package com.itsjeel01.remotevcsmanager.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.swing.Icon

@Composable
fun PlatformIcon(
    icon: Icon,
    modifier: Modifier = Modifier.size(16.dp),
    contentDescription: String? = null
) {
    val painter = remember(icon) {
        val w = icon.iconWidth.coerceAtLeast(1)
        val h = icon.iconHeight.coerceAtLeast(1)
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        icon.paintIcon(null, g, 0, 0)
        g.dispose()
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(baos.toByteArray())
        skiaImage?.let { BitmapPainter(it.toComposeImageBitmap()) }
    }
    if (painter != null) {
        Image(painter = painter, contentDescription = contentDescription, modifier = modifier)
    }
}
