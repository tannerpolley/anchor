package com.itsjeel01.remotevcsmanager.models

import java.util.Objects

/**
 * Data model for a remote repository.
 */
data class RemoteRepository(
    val id: String,
    val name: String,
    val fullName: String,
    val description: String?,
    val url: String,
    val cloneUrl: String,
    val defaultBranch: String,
    val isPrivate: Boolean,
    val owner: String,
    val provider: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteRepository) return false
        return id == other.id && provider == other.provider
    }

    override fun hashCode(): Int = Objects.hash(id, provider)
}
