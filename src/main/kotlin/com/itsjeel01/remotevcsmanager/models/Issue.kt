package com.itsjeel01.remotevcsmanager.models

/**
 * Data model for a GitHub issue.
 */
data class Issue(
    val id: String,
    val number: Int,
    val title: String,
    val body: String?,
    val state: IssueState,
    val url: String,
    val author: String,
    val assignees: List<String>,
    val labels: List<String>,
    val commentsCount: Int,
    val createdAt: String,
    val updatedAt: String,
    val isPullRequest: Boolean,
    val provider: String
)

enum class IssueState {
    OPEN,
    CLOSED;

    companion object {
        fun fromString(state: String): IssueState = when (state.lowercase()) {
            "open" -> OPEN
            "closed" -> CLOSED
            else -> OPEN
        }
    }
}

/**
 * Data model for an issue comment.
 */
data class IssueComment(
    val id: String,
    val body: String,
    val author: String,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Data model for a git branch.
 */
data class GitBranch(
    val name: String,
    val isDefault: Boolean,
    val sha: String,
    val commitUrl: String?
)
