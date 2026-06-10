package com.itsjeel01.remotevcsmanager.models

/**
 * Data model for a remote account (e.g., GitHub user).
 */
data class RemoteAccount(
    val id: String,
    val login: String,
    val name: String?,
    val email: String?,
    val avatarUrl: String?,
    val provider: String
)
