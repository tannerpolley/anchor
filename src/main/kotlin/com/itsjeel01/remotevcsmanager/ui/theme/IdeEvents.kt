package com.itsjeel01.remotevcsmanager.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.swing.UIManager

object IdeEvents {
    private val _theme = MutableStateFlow(0)
    val theme: StateFlow<Int> = _theme.asStateFlow()

    private val _fonts = MutableStateFlow(0)
    val fonts: StateFlow<Int> = _fonts.asStateFlow()

    private val _scale = MutableStateFlow(0)
    val scale: StateFlow<Int> = _scale.asStateFlow()

    init {
        UIManager.addPropertyChangeListener {
            _theme.value++
            _fonts.value++
            _scale.value++
        }
    }
}
