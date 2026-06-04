package com.itsjeel01.remotevcsmanager.providers.github

import com.itsjeel01.remotevcsmanager.settings.RemoteVcsSettingsState

/**
 * Handles GitHub Personal Access Token (PAT) storage and validation.
 */
class GitHubAuth {

    companion object {
        private const val PROVIDER_KEY = "github"
        private const val TOKEN_PREFIX = "Bearer "

        /**
         * Supported token formats for validation hints.
         */
        private val TOKEN_PATTERNS = listOf(
            Regex("^ghp_[a-zA-Z0-9]{36}$"),      // GitHub PAT (classic)
            Regex("^github_pat_[a-zA-Z0-9]{22}_[a-zA-Z0-9]{59}$"), // Fine-grained PATs
            Regex("^gho_[a-zA-Z0-9]{36}$"),       // Installation access token
            Regex("^ghu_[a-zA-Z0-9]{36}$")        // User-to-server token
        )
    }

    private val settings: RemoteVcsSettingsState = RemoteVcsSettingsState.getInstance()

    /**
     * Save a GitHub PAT to persistent settings.
     */
    fun saveToken(token: String) {
        settings.setToken(PROVIDER_KEY, token.trim())
    }

    /**
     * Retrieve the stored GitHub PAT.
     */
    fun getToken(): String? = settings.getToken(PROVIDER_KEY)

    /**
     * Remove the stored GitHub PAT.
     */
    fun clearToken() {
        settings.removeToken(PROVIDER_KEY)
    }

    /**
     * Check if a token has been saved.
     */
    fun hasToken(): Boolean = settings.hasToken(PROVIDER_KEY)

    /**
     * Basic format validation of a GitHub PAT.
     * This does NOT validate against the GitHub API.
     */
    fun isValidFormat(token: String): Boolean {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return false
        return TOKEN_PATTERNS.any { it.matches(trimmed) }
    }

    /**
     * Get the authorization header value for API calls.
     */
    fun getAuthorizationHeader(): String? {
        val token = getToken() ?: return null
        return "$TOKEN_PREFIX$token"
    }
}
