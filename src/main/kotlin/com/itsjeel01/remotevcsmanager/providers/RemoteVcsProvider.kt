package com.itsjeel01.remotevcsmanager.providers

import com.itsjeel01.remotevcsmanager.models.GitBranch
import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.models.PullRequest
import com.itsjeel01.remotevcsmanager.models.RemoteAccount
import com.itsjeel01.remotevcsmanager.models.RemoteRepository

/**
 * Abstract interface for remote VCS providers.
 * Implementations: GitHubProvider, GitLabProvider, BitbucketProvider, etc.
 */
abstract class RemoteVcsProvider {

    abstract val name: String
    abstract val key: String
    abstract val baseApiUrl: String
    abstract val baseWebUrl: String

    abstract fun setToken(token: String)
    abstract suspend fun validateToken(): Boolean
    abstract suspend fun getCurrentUser(): RemoteAccount
    abstract suspend fun getRepositories(): List<RemoteRepository>
    abstract suspend fun getRepository(owner: String, repo: String): RemoteRepository
    abstract suspend fun createRepository(name: String, description: String?, isPrivate: Boolean): RemoteRepository
    abstract suspend fun getPullRequests(owner: String, repo: String, state: String = "open"): List<PullRequest>

    // Issues API
    abstract suspend fun getIssues(
        owner: String,
        repo: String,
        state: String = "open",
        filter: String? = null,
        labels: String? = null
    ): List<Issue>

    abstract suspend fun createIssue(
        owner: String,
        repo: String,
        title: String,
        body: String? = null,
        labels: List<String>? = null,
        assignees: List<String>? = null
    ): Issue

    abstract suspend fun updateIssue(
        owner: String,
        repo: String,
        issueNumber: Int,
        state: String? = null,
        title: String? = null,
        body: String? = null
    ): Issue

    abstract suspend fun closeIssue(owner: String, repo: String, issueNumber: Int): Issue
    abstract suspend fun addIssueComment(owner: String, repo: String, issueNumber: Int, body: String): IssueComment
    abstract suspend fun getIssueComments(owner: String, repo: String, issueNumber: Int): List<IssueComment>

    // Branches API
    abstract suspend fun getBranches(owner: String, repo: String): List<GitBranch>

    abstract fun getFileUrl(owner: String, repo: String, filePath: String, branch: String, lineNumber: Int? = null): String
    abstract fun getCloneUrl(owner: String, repo: String, useSsh: Boolean = false): String
    open fun isConfigured(): Boolean = false
}
