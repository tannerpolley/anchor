package com.itsjeel01.remotevcsmanager.ui

import com.itsjeel01.remotevcsmanager.models.Issue

internal enum class IssueSortOption(
    private val label: String,
    val apiSort: String,
    val apiDirection: String
) {
    UPDATED_DESC("Updated newest", "updated", "desc"),
    UPDATED_ASC("Updated oldest", "updated", "asc"),
    NUMBER_DESC("Issue # newest", "created", "desc"),
    NUMBER_ASC("Issue # oldest", "created", "asc"),
    CREATED_DESC("Created newest", "created", "desc"),
    CREATED_ASC("Created oldest", "created", "asc"),
    MILESTONE("Milestone A-Z", "updated", "desc"),
    COMMENTS_DESC("Most commented", "comments", "desc"),
    COMMENTS_ASC("Least commented", "comments", "asc"),
    TITLE("Title A-Z", "updated", "desc");

    fun sort(issues: List<Issue>): List<Issue> =
        when (this) {
            UPDATED_DESC -> issues.sortedWith(
                compareByDescending<Issue> { it.updatedAt }
                    .thenByDescending { it.number }
            )
            UPDATED_ASC -> issues.sortedWith(compareBy<Issue> { it.updatedAt }.thenBy { it.number })
            NUMBER_DESC -> issues.sortedByDescending { it.number }
            NUMBER_ASC -> issues.sortedBy { it.number }
            CREATED_DESC -> issues.sortedWith(
                compareByDescending<Issue> { it.createdAt }
                    .thenByDescending { it.number }
            )
            CREATED_ASC -> issues.sortedWith(compareBy<Issue> { it.createdAt }.thenBy { it.number })
            MILESTONE -> issues.sortedWith(
                compareBy<Issue> { it.milestone.isNullOrBlank() }
                    .thenBy { it.milestone.orEmpty().lowercase() }
                    .thenByDescending { it.number }
            )
            COMMENTS_DESC -> issues.sortedWith(
                compareByDescending<Issue> { it.commentsCount }
                    .thenByDescending { it.number }
            )
            COMMENTS_ASC -> issues.sortedWith(compareBy<Issue> { it.commentsCount }.thenBy { it.number })
            TITLE -> issues.sortedWith(compareBy<Issue> { it.title.lowercase() }.thenBy { it.number })
        }

    override fun toString(): String = label
}
