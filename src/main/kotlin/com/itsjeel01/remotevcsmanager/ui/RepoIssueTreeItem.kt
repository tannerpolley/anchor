package com.itsjeel01.remotevcsmanager.ui

import com.itsjeel01.remotevcsmanager.models.Issue

internal sealed interface RepoIssueTreeItem {
    sealed interface SelectableIssue : RepoIssueTreeItem {
        val target: RepoIssueTarget
        val issue: Issue
    }

    data class ParentIssue(
        override val target: RepoIssueTarget,
        override val issue: Issue
    ) : SelectableIssue

    data class SubIssue(
        override val target: RepoIssueTarget,
        override val issue: Issue
    ) : SelectableIssue

    data class StandaloneIssue(
        override val target: RepoIssueTarget,
        override val issue: Issue
    ) : SelectableIssue

    data class Milestone(
        val target: RepoIssueTarget,
        val title: String,
        val openIssueCount: Int
    ) : RepoIssueTreeItem

    data class Message(
        val text: String
    ) : RepoIssueTreeItem
}
