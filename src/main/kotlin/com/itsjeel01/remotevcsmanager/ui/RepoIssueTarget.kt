package com.itsjeel01.remotevcsmanager.ui

internal data class RepoIssueTarget(
    val displayName: String,
    val owner: String,
    val repoName: String,
    val rootPath: String,
    val issuesUrl: String
)
