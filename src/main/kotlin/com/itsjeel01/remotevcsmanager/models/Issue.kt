package com.itsjeel01.remotevcsmanager.models

/**
 * Data model for a GitHub issue.
 */
data class Label(
    val name: String,
    val color: String  // hex color without #, e.g. "3FB950"
)

data class CommitSummary(
    val sha: String,
    val message: String,
    val url: String
)

data class Issue(
    val id: String,
    val number: Int,
    val title: String,
    val body: String?,
    val state: IssueState,
    val url: String,
    val author: String,
    val assignees: List<String>,
    val labels: List<Label>,
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
