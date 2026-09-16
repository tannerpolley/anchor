package com.itsjeel01.remotevcsmanager.models

data class IssueRelationship(
    val parentIssueNumber: Int,
    val childIssueNumber: Int
)

data class IssueDependency(
    val blockedIssueNumber: Int,
    val blockingIssue: Issue
)
