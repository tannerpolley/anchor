package com.itsjeel01.remotevcsmanager.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.ProjectManager
import com.itsjeel01.remotevcsmanager.GitRemoteDetector
import com.itsjeel01.remotevcsmanager.providers.github.GitHubProvider
import kotlinx.coroutines.runBlocking
import javax.swing.JComponent

class RemoteVcsConfigurable : Configurable {

    private var panel: RemoteVcsSettingsPanel? = null
    private val settings = RemoteVcsSettingsState.getInstance()
    private val provider = GitHubProvider()

    override fun getDisplayName(): String = "Remote VCS Manager"

    override fun createComponent(): JComponent {
        val p = RemoteVcsSettingsPanel(); panel = p

        val currentToken = settings.getToken("github") ?: ""
        p.setToken(currentToken)
        p.setAutoRefresh(settings.getAutoRefresh())

        // Detect remote and set provider-aware links
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        var detectedProvider = "github"
        if (project != null) {
            val det = GitRemoteDetector(project)
            val info = det.detect()
            val prov = info?.provider ?: "github"
            detectedProvider = prov
            p.updateRepoInfo(info?.owner, info?.repoName, info?.currentBranch, prov)
        }

        // Connected? Show status
        if (currentToken.isNotEmpty()) {
            p.setTokenStatus("Token stored", true)
            // Try to check if still valid
            val t = currentToken
            ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Checking connection...", false) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    try {
                        provider.setToken(t)
                        val ok = runBlocking { provider.validateToken() }
                        if (ok) {
                            val user = runBlocking { provider.getCurrentUser() }
                            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                                p.setConnected(true, user.login)
                            }
                        } else {
                            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                                p.setConnected(false)
                            }
                        }
                    } catch (_: Exception) {
                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                            p.setConnected(false)
                        }
                    }
                }
            })
        }

        // Validate button callback
        p.setOnValidateToken {
            val token = p.getToken()
            if (token.isBlank()) { p.setTokenStatus("Enter a token", false); return@setOnValidateToken }
            p.setValidateEnabled(false); p.setTokenStatus("Validating...", true)
            ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Validating...", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    try {
                        provider.setToken(token.trim()); val ok = runBlocking { provider.validateToken() }
                        val user = if (ok) runBlocking { provider.getCurrentUser() } else null
                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                            p.setValidateEnabled(true)
                            if (ok && user != null) {
                                p.setTokenStatus("✓ Authenticated as ${user.login}", true)
                                p.setConnected(true, user.login)
                                settings.setToken("github", token.trim())
                            } else { p.setTokenStatus("✗ Token rejected", false); p.setConnected(false) }
                        }
                    } catch (e: Exception) {
                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                            p.setValidateEnabled(true)
                            p.setTokenStatus("✗ ${e.message}", false)
                            p.setConnected(false)
                        }
                    }
                }
            })
        }

        return p.panel
    }

    override fun isModified(): Boolean {
        val p = panel ?: return false
        return p.isModified(settings.getToken("github") ?: "", settings.getAutoRefresh())
    }

    override fun apply() {
        val p = panel ?: return
        if (p.getToken().isNotBlank()) settings.setToken("github", p.getToken()) else settings.removeToken("github")
        settings.setAutoRefresh(p.getAutoRefresh())
        // Notify tool window to reload after token change
        SettingsChangeNotifier.publish()
    }

    override fun reset() {
        val p = panel ?: return
        p.setToken(settings.getToken("github") ?: "")
        val has = settings.hasToken("github")
        p.setTokenStatus(if (has) "Token stored" else "", true)
        p.setConnected(has, null)
        p.setAutoRefresh(settings.getAutoRefresh())
    }

    override fun disposeUIResources() { panel = null }
}
