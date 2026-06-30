package com.itsjeel01.remotevcsmanager.ui

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project

@State(
    name = "AnchorRepoIssueInclusion",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
@Service(Service.Level.PROJECT)
internal class RepoIssueInclusionSettings :
    SimplePersistentStateComponent<RepoIssueInclusionSettings.State>(State()) {

    class State : BaseState() {
        val excludedRepoKeys: MutableList<String> by list()
    }

    fun inclusionState(): RepoIssueInclusionState =
        RepoIssueInclusionState(state.excludedRepoKeys.toSet())

    fun setInclusionState(inclusionState: RepoIssueInclusionState): Unit {
        state.excludedRepoKeys.clear()
        state.excludedRepoKeys.addAll(inclusionState.excludedKeys.sorted())
    }

    companion object {
        fun getInstance(project: Project): RepoIssueInclusionSettings =
            project.getService(RepoIssueInclusionSettings::class.java)
    }
}
