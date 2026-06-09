package com.itsjeel01.remotevcsmanager.ui.theme

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.swing.UIManager

/**
 * Central event bus for IDE appearance changes (themes, fonts, scaling).
 *
 * Rapid property-change events from [UIManager] are debounced into a single
 * increment per counter, preventing recomposition storms during theme switches.
 */
object IdeEvents {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _theme = MutableStateFlow(0)
    val theme: StateFlow<Int> = _theme.asStateFlow()

    private val _fonts = MutableStateFlow(0)
    val fonts: StateFlow<Int> = _fonts.asStateFlow()

    private val _scale = MutableStateFlow(0)
    val scale: StateFlow<Int> = _scale.asStateFlow()

    private var themeDebounceJob: Job? = null
    private var pendingThemeChange = false
    private var pendingFontChange = false
    private var pendingScaleChange = false
    private val lock = Any()
    private val debounceMs = 80L

    init {
        UIManager.addPropertyChangeListener { e ->
            val prop = e.propertyName?.lowercase()
            val themeChanged = prop == null ||
                prop.contains("color") ||
                prop.contains("laf") ||
                prop.contains("lookandfeel") ||
                prop.contains("theme")
            val fontChanged = prop == null ||
                prop.contains("font") ||
                prop.contains("size")
            val scaleChanged = prop == null ||
                prop.contains("scale") ||
                prop.contains("dpi") ||
                prop.contains("ui.scale")

            if (themeChanged || fontChanged || scaleChanged) {
                synchronized(lock) {
                    pendingThemeChange = pendingThemeChange || themeChanged
                    pendingFontChange = pendingFontChange || fontChanged
                    pendingScaleChange = pendingScaleChange || scaleChanged
                    rescheduleFlushLocked()
                }
            }
        }
    }

    private fun rescheduleFlushLocked() {
        themeDebounceJob?.cancel()
        themeDebounceJob = scope.launch {
            delay(debounceMs)
            val themeChanged: Boolean
            val fontChanged: Boolean
            val scaleChanged: Boolean
            synchronized(lock) {
                themeChanged = pendingThemeChange
                fontChanged = pendingFontChange
                scaleChanged = pendingScaleChange
                pendingThemeChange = false
                pendingFontChange = false
                pendingScaleChange = false
                themeDebounceJob = null
            }
            if (themeChanged) _theme.update { it + 1 }
            if (fontChanged) _fonts.update { it + 1 }
            if (scaleChanged) _scale.update { it + 1 }
        }
    }
}
