package com.itsjeel01.remotevcsmanager.ui

import java.awt.Color as AwtColor

fun blend(fg: AwtColor, bg: AwtColor, weight: Float): AwtColor {
    val w = weight.coerceIn(0f, 1f)
    return AwtColor(
        (fg.red * w + bg.red * (1 - w)).toInt().coerceIn(0, 255),
        (fg.green * w + bg.green * (1 - w)).toInt().coerceIn(0, 255),
        (fg.blue * w + bg.blue * (1 - w)).toInt().coerceIn(0, 255)
    )
}
