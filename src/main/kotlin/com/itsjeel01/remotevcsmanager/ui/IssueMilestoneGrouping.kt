package com.itsjeel01.remotevcsmanager.ui

import com.itsjeel01.remotevcsmanager.models.Issue

object IssueMilestoneGrouping {
    const val NO_MILESTONE: String = "No Milestone"

    data class MilestoneGroup(
        val title: String,
        val issues: List<Issue>
    )

    fun group(issues: List<Issue>): List<MilestoneGroup> =
        issues.groupBy { issue ->
            issue.milestone?.takeIf { it.isNotBlank() } ?: NO_MILESTONE
        }.entries.sortedWith(
            compareBy<Map.Entry<String, List<Issue>>> { it.key == NO_MILESTONE }
                .thenBy { it.key.lowercase() }
        ).map { (title, groupIssues) ->
            MilestoneGroup(title, groupIssues)
        }
}
