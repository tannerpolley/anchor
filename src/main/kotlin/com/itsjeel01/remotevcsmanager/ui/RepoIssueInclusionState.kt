package com.itsjeel01.remotevcsmanager.ui

internal data class RepoIssueInclusionState(
    val excludedKeys: Set<String>
) {
    fun includedTargets(targets: List<RepoIssueTarget>): List<RepoIssueTarget> =
        targets.filter(::isIncluded)

    fun isIncluded(target: RepoIssueTarget): Boolean =
        keyFor(target) !in excludedKeys

    fun withIncluded(target: RepoIssueTarget, included: Boolean): RepoIssueInclusionState {
        val key = keyFor(target)
        return if (included) {
            copy(excludedKeys = excludedKeys - key)
        } else {
            copy(excludedKeys = excludedKeys + key)
        }
    }

    companion object {
        fun keyFor(target: RepoIssueTarget): String =
            listOf(target.owner, target.repoName, target.rootPath).joinToString("|")
    }
}
