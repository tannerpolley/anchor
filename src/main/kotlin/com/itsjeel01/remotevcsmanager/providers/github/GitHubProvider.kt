package com.itsjeel01.remotevcsmanager.providers.github

import com.itsjeel01.remotevcsmanager.models.CommitSummary
import com.itsjeel01.remotevcsmanager.models.GitBranch
import com.itsjeel01.remotevcsmanager.models.Issue
import com.itsjeel01.remotevcsmanager.models.IssueComment
import com.itsjeel01.remotevcsmanager.models.IssueMilestone
import com.itsjeel01.remotevcsmanager.models.IssueRelationship
import com.itsjeel01.remotevcsmanager.models.IssueState
import com.itsjeel01.remotevcsmanager.models.Label
import com.itsjeel01.remotevcsmanager.models.PRState
import com.itsjeel01.remotevcsmanager.models.PullRequest
import com.itsjeel01.remotevcsmanager.models.RemoteAccount
import com.itsjeel01.remotevcsmanager.models.RemoteRepository
import com.itsjeel01.remotevcsmanager.providers.RemoteVcsProvider
import com.itsjeel01.remotevcsmanager.ui.VcsCache

/**
 * GitHub implementation of the RemoteVcsProvider interface.
 * Uses GitHub REST API v3 via GitHubApiClient.
 */
class GitHubProvider(
    private val auth: GitHubAuth = GitHubAuth(),
    private val apiClient: GitHubApiClient = GitHubApiClient(auth)
) : RemoteVcsProvider() {

    override val name: String = "GitHub"
    override val key: String = "github"
    override val baseApiUrl: String = GitHubApiClient.API_BASE_URL
    override val baseWebUrl: String = GitHubApiClient.WEB_BASE_URL

    private var token: String? = null

    override fun setToken(token: String) {
        this.token = token
        auth.saveToken(token)
    }

    override fun isConfigured(): Boolean = auth.hasToken()

    override suspend fun validateToken(): Boolean = apiClient.validateToken()

    override suspend fun getCurrentUser(): RemoteAccount {
        val user = apiClient.getCurrentUser().getOrThrow()
        return RemoteAccount(
            id = apiClient.safeLong(user, "id").toString(),
            login = apiClient.safeString(user, "login") ?: "unknown",
            name = apiClient.safeString(user, "name"),
            email = apiClient.safeString(user, "email"),
            avatarUrl = apiClient.safeString(user, "avatar_url"),
            provider = key
        )
    }

    override suspend fun getRepositories(): List<RemoteRepository> {
        return apiClient.getRepositories().getOrThrow().map { json -> toRemoteRepository(json) }
    }

    override suspend fun getRepository(owner: String, repo: String): RemoteRepository {
        return toRemoteRepository(apiClient.getRepository(owner, repo).getOrThrow())
    }

    override suspend fun createRepository(name: String, description: String?, isPrivate: Boolean): RemoteRepository {
        return toRemoteRepository(apiClient.createRepository(name, description, isPrivate).getOrThrow())
    }

    override suspend fun getPullRequests(owner: String, repo: String, state: String): List<PullRequest> {
        return apiClient.getPullRequests(owner, repo, state).getOrThrow().map { json -> toPullRequest(json) }
    }

    override suspend fun getMilestones(owner: String, repo: String): List<IssueMilestone> =
        apiClient.getMilestones(owner, repo).getOrThrow().map { json ->
            GitHubIssueStructureParser.toMilestone(json)
        }

    override suspend fun getIssues(
        owner: String,
        repo: String,
        state: String,
        filter: String?,
        labels: String?
    ): List<Issue> {
        return getIssuesSorted(owner, repo, state, filter, labels, sort = "updated", direction = "desc")
    }

    suspend fun getIssuesSorted(
        owner: String,
        repo: String,
        state: String,
        filter: String?,
        labels: String?,
        sort: String,
        direction: String
    ): List<Issue> {
        return apiClient.getIssues(owner, repo, state, filter, labels, sort, direction)
            .getOrThrow()
            .map { json -> toIssue(json) }
            .filter { !it.isPullRequest }
    }

    override suspend fun getIssueRelationships(
        owner: String,
        repo: String,
        issues: List<Issue>
    ): List<IssueRelationship> =
        issues.flatMap { issue ->
            val subIssues = apiClient.getSubIssues(owner, repo, issue.number).getOrThrow()
            val jsonArray = com.google.gson.JsonArray().apply {
                subIssues.forEach { add(it) }
            }
            GitHubIssueStructureParser.toIssueRelationships(issue.number, jsonArray)
        }

    suspend fun getIssueTrackingAccess(
        owner: String,
        repo: String,
        accountLogins: Set<String>
    ): GitHubIssueTrackingAccess {
        val repository = apiClient.getRepository(owner, repo).getOrThrow()
        val repositoryOwner = apiClient.safeObject(repository, "owner")?.let {
            apiClient.safeString(it, "login")
        }.orEmpty().lowercase()
        val permissions = apiClient.safeObject(repository, "permissions")
        val hasAdminPermission = permissions?.let { apiClient.safeBoolean(it, "admin") } ?: false
        return GitHubIssueTrackingAccess(
            owned = repositoryOwner in accountLogins || hasAdminPermission,
            fork = apiClient.safeBoolean(repository, "fork")
        )
    }

    override suspend fun createIssue(
        owner: String, repo: String, title: String, body: String?,
        labels: List<String>?, assignees: List<String>?
    ): Issue {
        return toIssue(apiClient.createIssue(owner, repo, title, body, labels, assignees).getOrThrow())
    }

    override suspend fun updateIssue(
        owner: String, repo: String, issueNumber: Int,
        state: String?, title: String?, body: String?
    ): Issue {
        return toIssue(apiClient.updateIssue(owner, repo, issueNumber, state, title, body).getOrThrow())
    }

    override suspend fun closeIssue(owner: String, repo: String, issueNumber: Int): Issue {
        return updateIssue(owner, repo, issueNumber, state = "closed")
    }

    override suspend fun addIssueComment(owner: String, repo: String, issueNumber: Int, body: String): IssueComment {
        val json = apiClient.addIssueComment(owner, repo, issueNumber, body).getOrThrow()
        return IssueComment(
            id = apiClient.safeLong(json, "id").toString(),
            body = apiClient.safeString(json, "body") ?: "",
            author = apiClient.safeObject(json, "user")?.let {
                apiClient.safeString(it, "login")
            } ?: "unknown",
            createdAt = apiClient.safeString(json, "created_at") ?: "",
            updatedAt = apiClient.safeString(json, "updated_at") ?: ""
        )
    }

    override suspend fun getIssueComments(owner: String, repo: String, issueNumber: Int): List<IssueComment> {
        val cacheKey = "comments_${owner}_${repo}_$issueNumber"
        val cached: List<IssueComment>? = VcsCache.getApi(cacheKey)
        if (cached != null) return cached
        val result = apiClient.getIssueComments(owner, repo, issueNumber)
            .getOrThrow()
            .map { json ->
            val user = apiClient.safeObject(json, "user")
            IssueComment(
                id = apiClient.safeLong(json, "id").toString(),
                body = apiClient.safeString(json, "body") ?: "",
                author = if (user != null) apiClient.safeString(user, "login") ?: "unknown" else "unknown",
                createdAt = apiClient.safeString(json, "created_at") ?: "",
                updatedAt = apiClient.safeString(json, "updated_at") ?: ""
            )
        }
        VcsCache.putApi(cacheKey, result)
        return result
    }

    override suspend fun getBranches(owner: String, repo: String): List<GitBranch> {
        return apiClient.getBranches(owner, repo)
            .getOrThrow()
            .map { json ->
            val commit = apiClient.safeObject(json, "commit")
            GitBranch(
                name = apiClient.safeString(json, "name") ?: "unknown",
                isDefault = false,  // We can't know default from this endpoint alone
                sha = if (commit != null) apiClient.safeString(commit, "sha") ?: "" else "",
                commitUrl = if (commit != null) apiClient.safeString(commit, "url") else null
            )
        }
    }

    override suspend fun getPullRequestCommits(owner: String, repo: String, prNumber: Int): List<CommitSummary> {
        val cacheKey = "commits_${owner}_${repo}_$prNumber"
        val cached: List<CommitSummary>? = VcsCache.getApi(cacheKey)
        if (cached != null) return cached
        val result = apiClient.getPullRequestCommits(owner, repo, prNumber)
            .getOrThrow()
            .map { json ->
            val commit = apiClient.safeObject(json, "commit")
            val committer = if (commit != null) apiClient.safeObject(commit, "committer") else null
            val author = if (commit != null) {
                val commitAuthor = apiClient.safeObject(commit, "author")
                commitAuthor?.let { apiClient.safeString(it, "name") }
                    ?: apiClient.safeString(commit, "author")?.let {
                        // `author` field on commit object is sometimes the person object
                        null
                    }
            } else null
            val authoredAt = committer?.let { apiClient.safeString(it, "date") }
                ?: (commit?.let { apiClient.safeString(it, "committer_date") })
            CommitSummary(
                sha = apiClient.safeString(json, "sha") ?: "",
                message = if (commit != null) apiClient.safeString(commit, "message")?.lines()?.firstOrNull() ?: "" else "",
                url = apiClient.safeString(json, "html_url") ?: "",
                author = author,
                createdAt = authoredAt
            )
        }
        VcsCache.putApi(cacheKey, result)
        return result
    }

    override suspend fun getLabels(owner: String, repo: String): List<Label> {
        return apiClient.getLabels(owner, repo)
            .getOrThrow()
            .map { json ->
            Label(
                name = apiClient.safeString(json, "name") ?: "",
                color = apiClient.safeString(json, "color") ?: "888888"
            )
        }
    }

    override fun getFileUrl(owner: String, repo: String, filePath: String, branch: String, lineNumber: Int?): String =
        apiClient.getFileUrl(owner, repo, filePath, branch, lineNumber)

    override fun getCloneUrl(owner: String, repo: String, useSsh: Boolean): String =
        apiClient.getCloneUrl(owner, repo, useSsh)

    override suspend fun renderMarkdown(markdown: String, context: String?): String {
        val result = apiClient.renderMarkdown(markdown, context)
        return result.getOrDefault(markdown)
    }

    private fun toRemoteRepository(json: com.google.gson.JsonObject): RemoteRepository {
        val owner = apiClient.safeObject(json, "owner")
        return RemoteRepository(
            id = apiClient.safeLong(json, "id").toString(),
            name = apiClient.safeString(json, "name") ?: "unknown",
            fullName = apiClient.safeString(json, "full_name") ?: "unknown",
            description = apiClient.safeString(json, "description"),
            url = apiClient.safeString(json, "html_url") ?: "",
            cloneUrl = apiClient.safeString(json, "clone_url") ?: "",
            defaultBranch = apiClient.safeString(json, "default_branch") ?: "main",
            isPrivate = apiClient.safeBoolean(json, "private"),
            owner = if (owner != null) apiClient.safeString(owner, "login") ?: "unknown" else "unknown",
            provider = key
        )
    }

    private fun toPullRequest(json: com.google.gson.JsonObject): PullRequest {
        val user = apiClient.safeObject(json, "user")
        val head = apiClient.safeObject(json, "head")
        val base = apiClient.safeObject(json, "base")

        val mergedAt = json.get("merged_at")
        val isMerged = mergedAt != null && !(mergedAt is com.google.gson.JsonNull) && mergedAt.asString.isNotBlank()
        return PullRequest(
            id = apiClient.safeLong(json, "id").toString(),
            number = json.get("number")?.asInt ?: 0,
            title = apiClient.safeString(json, "title") ?: "Untitled",
            description = apiClient.safeString(json, "body"),
            url = apiClient.safeString(json, "html_url") ?: "",
            author = if (user != null) apiClient.safeString(user, "login") ?: "unknown" else "unknown",
            state = parsePRState(apiClient.safeString(json, "state") ?: "open", isMerged),
            sourceBranch = if (head != null) apiClient.safeString(head, "ref") ?: "unknown" else "unknown",
            targetBranch = if (base != null) apiClient.safeString(base, "ref") ?: "unknown" else "unknown",
            createdAt = apiClient.safeString(json, "created_at") ?: "",
            updatedAt = apiClient.safeString(json, "updated_at") ?: "",
            provider = key
        )
    }

    private fun toIssue(json: com.google.gson.JsonObject): Issue {
        val user = apiClient.safeObject(json, "user")
        val labelsArr = if (json.has("labels") && !(json.get("labels") is com.google.gson.JsonNull)) {
            json.getAsJsonArray("labels")?.mapNotNull {
                val labelObj = it.asJsonObject
                val name = apiClient.safeString(labelObj, "name") ?: return@mapNotNull null
                val color = apiClient.safeString(labelObj, "color") ?: "888888"
                Label(name = name, color = color)
            } ?: emptyList()
        } else emptyList()

        val assigneesArr = if (json.has("assignees") && !(json.get("assignees") is com.google.gson.JsonNull)) {
            json.getAsJsonArray("assignees")?.mapNotNull {
                val assigneeObj = it.asJsonObject
                apiClient.safeString(assigneeObj, "login")
            } ?: emptyList()
        } else emptyList()

        val prField = apiClient.safeObject(json, "pull_request")
        val milestone = apiClient.safeObject(json, "milestone")?.let {
            apiClient.safeString(it, "title")
        }
        return Issue(
            id = apiClient.safeLong(json, "id").toString(),
            number = json.get("number").asInt,
            title = apiClient.safeString(json, "title") ?: "Untitled",
            body = apiClient.safeString(json, "body"),
            state = IssueState.fromString(apiClient.safeString(json, "state") ?: "open"),
            url = apiClient.safeString(json, "html_url") ?: "",
            author = if (user != null) apiClient.safeString(user, "login") ?: "unknown" else "unknown",
            assignees = assigneesArr,
            labels = labelsArr,
            commentsCount = apiClient.safeLong(json, "comments", 0).toInt(),
            createdAt = apiClient.safeString(json, "created_at") ?: "",
            updatedAt = apiClient.safeString(json, "updated_at") ?: "",
            isPullRequest = prField != null,
            provider = key,
            milestone = milestone
        )
    }

    private fun parsePRState(state: String, merged: Boolean): PRState = when {
        merged -> PRState.MERGED
        state == "closed" -> PRState.CLOSED
        else -> PRState.OPEN
    }
}

data class GitHubIssueTrackingAccess(
    val owned: Boolean,
    val fork: Boolean
)
