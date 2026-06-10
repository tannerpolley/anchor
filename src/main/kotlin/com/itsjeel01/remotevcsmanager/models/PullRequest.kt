package com.itsjeel01.remotevcsmanager.models

/**
 * Data model for a pull request.
 */
data class PullRequest(
    val id: String,
    val number: Int,
    val title: String,
    val description: String?,
    val url: String,
    val author: String,
    val state: PRState,
    val sourceBranch: String,
    val targetBranch: String,
    val createdAt: String,
    val updatedAt: String,
    val provider: String
)

enum class PRState {
    OPEN,
    CLOSED,
    MERGED;

    companion object {
        fun fromString(state: String): PRState = when (state.lowercase()) {
            "open" -> OPEN
            "closed" -> CLOSED
            "merged" -> MERGED
            else -> OPEN
        }
    }
}
