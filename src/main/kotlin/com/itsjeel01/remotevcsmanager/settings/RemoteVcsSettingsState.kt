package com.itsjeel01.remotevcsmanager.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XMap

/**
 * Persistent state for the Remote VCS Manager plugin.
 * Stores tokens, preferences, and provider settings.
 */
@State(
    name = "RemoteVcsSettings",
    storages = [Storage("remoteVcsManagerSettings.xml")]
)
@Service(Service.Level.APP)
final class RemoteVcsSettingsState :
    SimplePersistentStateComponent<RemoteVcsSettingsState.State>(State()) {

    class State : BaseState() {
        @get:XMap
        @get:Tag("providerTokens")
        val providerTokens: MutableMap<String, String> by map()

        var defaultProvider: String? by string("github")
        var autoRefresh: Boolean by property(true)
        var defaultFilterIndex: Int by property(0)
        var issueFetchLimit: String? by string("50")
        var defaultTokenUrl: String? by string("https://github.com/settings/tokens")
    }

    fun getToken(provider: String): String? = state.providerTokens[provider]

    fun setToken(provider: String, token: String) {
        state.providerTokens[provider] = token
    }

    fun removeToken(provider: String) {
        state.providerTokens.remove(provider)
    }

    fun hasToken(provider: String): Boolean =
        state.providerTokens.containsKey(provider) &&
            state.providerTokens[provider]?.isNotEmpty() == true

    fun getDefaultProvider(): String = state.defaultProvider ?: "github"
    fun setDefaultProvider(provider: String) { state.defaultProvider = provider }

    fun getAutoRefresh(): Boolean = state.autoRefresh
    fun setAutoRefresh(value: Boolean) { state.autoRefresh = value }

    fun getDefaultFilterIndex(): Int = state.defaultFilterIndex
    fun setDefaultFilterIndex(index: Int) { state.defaultFilterIndex = index }

    fun getIssueFetchLimit(): String = state.issueFetchLimit ?: "50"
    fun setIssueFetchLimit(limit: String) { state.issueFetchLimit = limit }

    companion object {
        @JvmStatic
        fun getInstance(): RemoteVcsSettingsState =
            ApplicationManager.getApplication().getService(RemoteVcsSettingsState::class.java)
    }
}
