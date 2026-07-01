package com.itsjeel01.remotevcsmanager.ui

import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueMilestone
import com.itsjeel01.remotevcsmanager.models.IssueRelationship

internal object IssueTreeGrouping {
    const val NO_MILESTONE: String = "No Milestone"

    data class MilestoneGroup(
        val title: String,
        val openIssueCount: Int,
        val rows: List<IssueRow>
    )

    sealed interface IssueRow {
        val issue: Issue

        data class Parent(
            override val issue: Issue,
            val children: List<Issue>
        ) : IssueRow

        data class Standalone(
            override val issue: Issue
        ) : IssueRow
    }

    fun group(
        milestones: List<IssueMilestone>,
        issues: List<Issue>,
        relationships: List<IssueRelationship>
    ): List<MilestoneGroup> {
        val sortedMilestones = milestones
            .distinctBy { it.title }
            .sortedBy { it.title.lowercase() }
        val officialMilestoneTitles = sortedMilestones.mapTo(mutableSetOf()) { it.title }
        val rowsByMilestone = linkedMapOf<String, MutableList<IssueRow>>()
        sortedMilestones.forEach { milestone ->
            rowsByMilestone[milestone.title] = mutableListOf()
        }

        val issuesByNumber = issues.associateBy { it.number }
        val childrenByParent = relationships
            .groupBy { it.parentIssueNumber }
            .mapValues { (_, parentRelationships) ->
                parentRelationships.mapNotNull { relationship ->
                    issuesByNumber[relationship.childIssueNumber]
                }.distinctBy { it.number }
            }.filterValues { it.isNotEmpty() }
        val childIssueNumbers = childrenByParent.values
            .flatten()
            .mapTo(mutableSetOf()) { it.number }

        issues.forEach { issue ->
            if (issue.number in childIssueNumbers) return@forEach
            val title = issue.milestone?.takeIf { it.isNotBlank() } ?: NO_MILESTONE
            val children = childrenByParent[issue.number].orEmpty()
            val row = if (children.isEmpty()) {
                IssueRow.Standalone(issue)
            } else {
                IssueRow.Parent(issue, children)
            }
            rowsByMilestone.getOrPut(title) { mutableListOf() }.add(row)
        }

        val groups = mutableListOf<MilestoneGroup>()
        sortedMilestones.forEach { milestone ->
            groups.add(
                MilestoneGroup(
                    title = milestone.title,
                    openIssueCount = milestone.openIssueCount,
                    rows = rowsByMilestone[milestone.title].orEmpty()
                )
            )
        }

        rowsByMilestone.keys
            .filter { it !in officialMilestoneTitles && it != NO_MILESTONE }
            .sortedBy { it.lowercase() }
            .forEach { title ->
                val rows = rowsByMilestone.getValue(title)
                groups.add(MilestoneGroup(title = title, openIssueCount = countIssues(rows), rows = rows))
            }

        val noMilestoneRows = rowsByMilestone[NO_MILESTONE].orEmpty()
        if (noMilestoneRows.isNotEmpty()) {
            groups.add(
                MilestoneGroup(
                    title = NO_MILESTONE,
                    openIssueCount = countIssues(noMilestoneRows),
                    rows = noMilestoneRows
                )
            )
        }
        return groups
    }

    private fun countIssues(rows: List<IssueRow>): Int =
        rows.sumOf { row ->
            when (row) {
                is IssueRow.Parent -> 1 + row.children.size
                is IssueRow.Standalone -> 1
            }
        }
}
