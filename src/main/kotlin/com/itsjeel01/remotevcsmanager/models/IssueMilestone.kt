package com.itsjeel01.remotevcsmanager.models

data class IssueMilestone(
    val id: String,
    val title: String,
    val openIssueCount: Int,
    val state: String
)
