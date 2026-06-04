package com.itsjeel01.remotevcsmanager.settings

import com.intellij.ide.BrowserUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Cursor
import java.awt.Font
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel

class RemoteVcsSettingsPanel {

    private val statusDot = JLabel("○").apply {
        font = font.deriveFont(Font.BOLD, 10f)
        foreground = UIUtil.getContextHelpForeground()
    }
    private val statusText = JBLabel("Not connected").apply { foreground = UIUtil.getContextHelpForeground() }

    private val tokenField = JBPasswordField().apply { emptyText.text = "Paste your Personal Access Token" }
    private val tokenStatus = JBLabel().apply {
        foreground = UIUtil.getContextHelpForeground()
        font = JBUI.Fonts.smallFont()
    }
    private val validateBtn = JButton("Validate").apply {
        isOpaque = false
        addActionListener { onValidateToken() }
    }
    private val clearBtn = JButton("Clear Token").apply {
        isOpaque = false
        addActionListener { tokenField.text = ""; setTokenStatus("Cleared", false) }
    }

    private val tokenLink = JLabel().apply {
        foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
        font = JBUI.Fonts.label(11f)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }

    private val repoNameLabel = JBLabel("No remote detected").apply {
        foreground = UIUtil.getContextHelpForeground()
        font = JBUI.Fonts.label(12f)
    }
    private val branchLabel = JLabel().apply {
        font = JBUI.Fonts.smallFont()
        foreground = UIUtil.getContextHelpForeground()
    }
    private val autoRefreshCb = JCheckBox("Auto-refresh on project open", true)

    private var validateCallback: (() -> Unit)? = null

    // Using Kotlin UI DSL v2 for native IDEA layout proportions
    val panel: JComponent = panel {
        group("Connection") {
            row {
                cell(statusDot).gap(RightGap.SMALL)
                cell(statusText)
            }
        }
        group("Repository") {
            row {
                cell(repoNameLabel)
            }
            row {
                cell(branchLabel)
            }
        }
        group("Authentication") {
            row("Token:") {
                cell(tokenField)
                    .align(AlignX.FILL)
                    .resizableColumn()
                cell(validateBtn)
                cell(clearBtn)
            }
            // Passing an empty string pushes the helper text/link perfectly under the text field
            row("") {
                cell(tokenStatus)
            }
            row("") {
                cell(tokenLink)
            }
        }
        group("Preferences") {
            row {
                cell(autoRefreshCb)
            }
        }
    }

    fun getToken() = String(tokenField.password)

    fun setToken(t: String) { tokenField.text = t }

    fun setTokenStatus(msg: String, ok: Boolean) {
        tokenStatus.text = if (msg.isEmpty()) "" else msg
        tokenStatus.foreground = if (ok) UIUtil.getActiveTextColor() else UIUtil.getErrorForeground()
    }

    fun setValidateEnabled(v: Boolean) { validateBtn.isEnabled = v }

    fun setOnValidateToken(cb: () -> Unit) { validateCallback = cb }

    private fun onValidateToken() { validateCallback?.invoke() }

    fun isModified(curToken: String, curRefresh: Boolean) =
        getToken() != curToken || autoRefreshCb.isSelected != curRefresh

    fun getAutoRefresh() = autoRefreshCb.isSelected

    fun setAutoRefresh(v: Boolean) { autoRefreshCb.isSelected = v }

    fun setConnected(connected: Boolean, user: String? = null) {
        if (connected && user != null) {
            statusDot.text = "●"
            statusDot.foreground = com.intellij.ui.JBColor(0x2DA44E, 0x3FB950)
            statusText.text = "Connected as $user"
            statusText.foreground = UIUtil.getActiveTextColor()
        } else {
            statusDot.text = "○"
            statusDot.foreground = UIUtil.getContextHelpForeground()
            statusText.text = "Not connected"
            statusText.foreground = UIUtil.getContextHelpForeground()
        }
    }

    fun updateRepoInfo(owner: String?, repo: String?, branch: String?, detectedProvider: String = "github") {
        if (owner != null && repo != null) {
            repoNameLabel.text = "$owner/$repo"
            repoNameLabel.foreground = UIUtil.getActiveTextColor()
            branchLabel.text = branch ?: "unknown"
            val url = buildTokenUrl(detectedProvider, owner, repo)
            setLink(tokenLink, url, "Generate a token for $owner/$repo \u2192")
        } else {
            repoNameLabel.text = "No remote detected"
            repoNameLabel.foreground = UIUtil.getContextHelpForeground()
            branchLabel.text = ""
            val url = buildTokenUrl(detectedProvider, null, null)
            setLink(tokenLink, url, "Create a personal access token \u2192")
        }
    }

    private fun buildTokenUrl(provider: String, owner: String?, repo: String?): String {
        val desc = if (owner != null && repo != null) "RemoteVCSManager+$owner/$repo" else "RemoteVCSManager"
        return when (provider) {
            "github" -> "https://github.com/settings/tokens/new?scopes=repo,read:user,read:org&description=$desc"
            "gitlab" -> "https://gitlab.com/-/user_settings/personal_access_tokens?name=$desc&scopes=api,read_repository,read_user"
            "bitbucket" -> "https://bitbucket.org/account/settings/app-passwords/new"
            "azure" -> "https://dev.azure.com/_usersSettings/tokens"
            else -> "https://github.com/settings/tokens/new?scopes=repo,read:user,read:org&description=$desc"
        }
    }

    private fun setLink(label: JLabel, url: String, labelText: String) {
        label.text = "<html>$labelText</html>"
        for (l in label.mouseListeners.toList()) label.removeMouseListener(l)
        label.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) { BrowserUtil.browse(url) }
        })
    }
}