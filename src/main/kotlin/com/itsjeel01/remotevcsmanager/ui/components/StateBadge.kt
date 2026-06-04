package com.itsjeel01.remotevcsmanager.ui.components

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Pill-shaped state badge.
 * Wraps content tightly — does NOT stretch full width.
 */
class StateBadge : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {

    private val label = JLabel()

    init {
        isOpaque = false
        label.font = JBUI.Fonts.smallFont()
        label.border = BorderFactory.createEmptyBorder(1, 6, 1, 6)
        add(label)
    }

    fun setIssueState(state: com.itsjeel01.remotevcsmanager.models.IssueState) {
        when (state) {
            com.itsjeel01.remotevcsmanager.models.IssueState.OPEN -> configure("● Open", JBColor(0x2DA44E, 0x3FB950))
            com.itsjeel01.remotevcsmanager.models.IssueState.CLOSED -> configure("● Closed", JBColor(0x8957E5, 0x8957E5))
        }
    }

    fun setPRState(state: com.itsjeel01.remotevcsmanager.models.PRState) {
        when (state) {
            com.itsjeel01.remotevcsmanager.models.PRState.OPEN -> configure("● Open", JBColor(0x2DA44E, 0x3FB950))
            com.itsjeel01.remotevcsmanager.models.PRState.CLOSED -> configure("● Closed", JBColor(0xCF222E, 0xF85149))
            com.itsjeel01.remotevcsmanager.models.PRState.MERGED -> configure("● Merged", JBColor(0x8957E5, 0x8957E5))
        }
    }

    private fun configure(text: String, color: Color) {
        label.text = text
        label.foreground = color
        label.background = Color(color.red, color.green, color.blue, 25)
        label.isOpaque = true
        label.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color(color.red, color.green, color.blue, 60), 1),
            BorderFactory.createEmptyBorder(1, 6, 1, 6)
        )
        // Recalculate preferred size
        label.revalidate()
    }
}
