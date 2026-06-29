package com.itsjeel01.remotevcsmanager.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RepoIssueInclusionStateTest {

    @Test
    fun newlyDetectedTargetsDefaultIncluded(): Unit {
        val target = targetFixture("anchor")

        val state = RepoIssueInclusionState(emptySet())

        assertTrue(state.isIncluded(target))
        assertEquals(listOf(target), state.includedTargets(listOf(target)))
    }

    @Test
    fun excludedTargetIsRemovedFromIncludedTargets(): Unit {
        val included = targetFixture("anchor")
        val excluded = targetFixture("superpowers-project")
        val state = RepoIssueInclusionState(
            excludedKeys = setOf(RepoIssueInclusionState.keyFor(excluded))
        )

        assertFalse(state.isIncluded(excluded))
        assertEquals(listOf(included), state.includedTargets(listOf(included, excluded)))
    }

    @Test
    fun staleExcludedTargetDoesNotAffectCurrentTargets(): Unit {
        val current = targetFixture("anchor")
        val state = RepoIssueInclusionState(excludedKeys = setOf("tannerpolley|missing|C:/missing"))

        assertEquals(listOf(current), state.includedTargets(listOf(current)))
    }

    @Test
    fun changingInclusionUpdatesExcludedKeys(): Unit {
        val anchor = targetFixture("anchor")
        val bridge = targetFixture("jetbrains-bridge")

        val state = RepoIssueInclusionState(emptySet())
            .withIncluded(anchor, included = false)
            .withIncluded(bridge, included = true)

        assertEquals(setOf(RepoIssueInclusionState.keyFor(anchor)), state.excludedKeys)
    }

    private fun targetFixture(repoName: String): RepoIssueTarget =
        RepoIssueTarget(
            displayName = repoName,
            owner = "tannerpolley",
            repoName = repoName,
            rootPath = "C:/workspaces/$repoName",
            issuesUrl = "https://github.com/tannerpolley/$repoName/issues"
        )
}
