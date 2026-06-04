package com.itsjeel01.remotevcsmanager.ui.components

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Inline pill component for branch names — monospace + icon.
 */
class BranchPill(branchName: String) : JPanel(FlowLayout(FlowLayout.LEFT, 2, 0)) {

    init {
        isOpaque = false
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIUtil.getBoundsColor(), 1),
            BorderFactory.createEmptyBorder(1, 4, 1, 4)
        )

        val icon = JLabel(AllIcons.Vcs.Branch)
        icon.border = JBUI.Borders.emptyRight(2)
        add(icon)

        val label = JBLabel(truncateBranch(branchName, 30)).apply {
            font = Font(Font.MONOSPACED, Font.PLAIN, JBUI.Fonts.smallFont().size)
            foreground = UIUtil.getLabelForeground()
        }
        add(label)

        toolTipText = branchName
        preferredSize = Dimension(preferredSize.width + 8, JBUI.scale(18))
    }

    private fun truncateBranch(name: String, maxLen: Int): String {
        return if (name.length > maxLen) name.take(maxLen - 3) + "..." else name
    }
}
