package com.itsjeel01.remotevcsmanager.providers.github

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * OkHttp-based client for the GitHub REST API v3.
 */
class GitHubApiClient(
    private val auth: GitHubAuth = GitHubAuth()
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Safe JSON body parser that handles null/empty responses.
     */
    private fun parseJson(body: String): JsonElement {
        return try {
            JsonParser.parseString(body)
        } catch (e: JsonSyntaxException) {
            throw GitHubApiException(0, "Invalid JSON response: ${e.message}")
        }
    }

    /**
     * Safely get a String from a JsonObject field, handling JsonNull.
     */
    fun safeString(json: JsonObject, key: String): String? {
        val element = json.get(key)
        return if (element == null || element is JsonNull) null else element.asString
    }

    /**
     * Safely get a Boolean from a JsonObject field, handling JsonNull.
     */
    fun safeBoolean(json: JsonObject, key: String, default: Boolean = false): Boolean {
        val element = json.get(key)
        return if (element == null || element is JsonNull) default else element.asBoolean
    }

    /**
     * Safely get a Long from a JsonObject field, handling JsonNull.
     */
    fun safeLong(json: JsonObject, key: String, default: Long = 0L): Long {
        val element = json.get(key)
        return if (element == null || element is JsonNull) default else element.asLong
    }

    /**
     * Safely get a nested JsonObject, handling JsonNull.
     */
    fun safeObject(json: JsonObject, key: String): JsonObject? {
        val element = json.get(key)
        return if (element == null || element is JsonNull) null else element.asJsonObject
    }

    /**
     * Base URL for the GitHub REST API.
     */
    companion object {
        const val API_BASE_URL = "https://api.github.com"
        const val WEB_BASE_URL = "https://github.com"
        const val ACCEPT_HEADER = "application/vnd.github.v3+json"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Build an authenticated GET request.
     */
    private fun buildGetRequest(path: String): Request {
        val url = "$API_BASE_URL$path"
        return Request.Builder()
            .url(url)
            .addHeader("Accept", ACCEPT_HEADER)
            .addHeader("Authorization", auth.getAuthorizationHeader() ?: "")
            .addHeader("User-Agent", "RemoteVcsManager-JetBrains-Plugin")
            .get()
            .build()
    }

    /**
     * Build an authenticated POST request with optional JSON body.
     */
    private fun buildPostRequest(path: String, body: String? = null): Request {
        val url = "$API_BASE_URL$path"
        val requestBody = body?.toRequestBody(JSON_MEDIA_TYPE)
        return Request.Builder()
            .url(url)
            .addHeader("Accept", ACCEPT_HEADER)
            .addHeader("Authorization", auth.getAuthorizationHeader() ?: "")
            .addHeader("User-Agent", "RemoteVcsManager-JetBrains-Plugin")
            .post(requestBody ?: "".toRequestBody(JSON_MEDIA_TYPE))
            .build()
    }

    /**
     * Build an authenticated PATCH request with optional JSON body.
     */
    private fun buildPatchRequest(path: String, body: String): Request {
        val url = "$API_BASE_URL$path"
        val requestBody = body.toRequestBody(JSON_MEDIA_TYPE)
        return Request.Builder()
            .url(url)
            .addHeader("Accept", ACCEPT_HEADER)
            .addHeader("Authorization", auth.getAuthorizationHeader() ?: "")
            .addHeader("User-Agent", "RemoteVcsManager-JetBrains-Plugin")
            .patch(requestBody)
            .build()
    }

    /**
     * Execute a request and return the response body string.
     */
    private fun executeRequest(request: Request): Result<String> {
        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Result.success(body)
            } else {
                val errorMessage = when (response.code) {
                    401 -> "Authentication failed. Check your GitHub token."
                    403 -> "Access forbidden. Your token may lack permissions."
                    404 -> "Resource not found."
                    422 -> "Validation failed: $body"
                    else -> "GitHub API error (${response.code}): $body"
                }
                Result.failure(GitHubApiException(response.code, errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(GitHubApiException(0, "Network error: ${e.message}"))
        }
    }

    /**
     * Validate the current token by fetching the authenticated user.
     */
    fun validateToken(): Boolean {
        val request = buildGetRequest("/user")
        return executeRequest(request).isSuccess
    }

    /**
     * Get the authenticated user's profile.
     */
    fun getCurrentUser(): Result<JsonObject> {
        val request = buildGetRequest("/user")
        return executeRequest(request).map { body ->
            parseJson(body).asJsonObject
        }
    }

    /**
     * List repositories for the authenticated user.
     * Handles pagination.
     */
    fun getRepositories(): Result<List<JsonObject>> {
        val repos = mutableListOf<JsonObject>()
        var page = 1
        var hasMore = true

        while (hasMore) {
            val request = buildGetRequest("/user/repos?per_page=100&page=$page&sort=updated&type=all")
            val result = executeRequest(request)
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull()!!)
            }
            val body = result.getOrNull() ?: break
            val pageRepos = parseJson(body).asJsonArray
            if (pageRepos.size() == 0) {
                hasMore = false
            } else {
                pageRepos.forEach { repos.add(it.asJsonObject) }
                page++
            }
        }

        return Result.success(repos)
    }

    /**
     * Get a specific repository by owner and name.
     */
    fun getRepository(owner: String, repo: String): Result<JsonObject> {
        val path = "/repos/${encodePath(owner)}/${encodePath(repo)}"
        val request = buildGetRequest(path)
        return executeRequest(request).map { body ->
            parseJson(body).asJsonObject
        }
    }

    /**
     * Create a new repository for the authenticated user.
     */
    fun createRepository(
        name: String,
        description: String? = null,
        isPrivate: Boolean = false
    ): Result<JsonObject> {
        val jsonBody = JsonObject().apply {
            addProperty("name", name)
            description?.let { addProperty("description", it) }
            addProperty("private", isPrivate)
            addProperty("auto_init", true)
        }
        val request = buildPostRequest("/user/repos", gson.toJson(jsonBody))
        return executeRequest(request).map { body ->
            parseJson(body).asJsonObject
        }
    }

    /**
     * List issues for a given repository.
     */
    fun getIssues(
        owner: String,
        repo: String,
        state: String = "open",
        filter: String? = null,
        labels: String? = null,
        sort: String = "updated",
        direction: String = "desc"
    ): Result<List<JsonObject>> {
        var path = "/repos/${encodePath(owner)}/${encodePath(repo)}/issues?state=$state&sort=$sort&direction=$direction&per_page=50"
        if (filter != null) path += "&filter=$filter"
        if (labels != null) path += "&labels=$labels"
        val request = buildGetRequest(path)
        return executeRequest(request).map { body ->
            parseJson(body).asJsonArray.map { it.asJsonObject }
        }
    }

    /**
     * Create an issue.
     */
    fun createIssue(
        owner: String,
        repo: String,
        title: String,
        body: String? = null,
        labels: List<String>? = null,
        assignees: List<String>? = null
    ): Result<JsonObject> {
        val jsonBody = JsonObject().apply {
            addProperty("title", title)
            body?.let { addProperty("body", it) }
            labels?.let {
                val arr = JsonArray()
                it.forEach { label -> arr.add(label) }
                add("labels", arr)
            }
            assignees?.let {
                val arr = JsonArray()
                it.forEach { assignee -> arr.add(assignee) }
                add("assignees", arr)
            }
        }
        val request = buildPostRequest("/repos/${encodePath(owner)}/${encodePath(repo)}/issues", gson.toJson(jsonBody))
        return executeRequest(request).map { body ->
            parseJson(body).asJsonObject
        }
    }

    /**
     * Update an issue (close, change title, etc).
     */
    fun updateIssue(
        owner: String,
        repo: String,
        issueNumber: Int,
        state: String? = null,
        title: String? = null,
        body: String? = null
    ): Result<JsonObject> {
        val jsonBody = JsonObject()
        state?.let { jsonBody.addProperty("state", it) }
        title?.let { jsonBody.addProperty("title", it) }
        body?.let { jsonBody.addProperty("body", it) }
        val request = buildPatchRequest(
            "/repos/${encodePath(owner)}/${encodePath(repo)}/issues/$issueNumber",
            gson.toJson(jsonBody)
        )
        return executeRequest(request).map { body ->
            parseJson(body).asJsonObject
        }
    }

    /**
     * Add a comment to an issue.
     */
    fun addIssueComment(
        owner: String,
        repo: String,
        issueNumber: Int,
        body: String
    ): Result<JsonObject> {
        val jsonBody = JsonObject().apply {
            addProperty("body", body)
        }
        val request = buildPostRequest(
            "/repos/${encodePath(owner)}/${encodePath(repo)}/issues/$issueNumber/comments",
            gson.toJson(jsonBody)
        )
        return executeRequest(request).map { body ->
            parseJson(body).asJsonObject
        }
    }

    /**
     * List comments on an issue.
     */
    fun getIssueComments(
        owner: String,
        repo: String,
        issueNumber: Int
    ): Result<List<JsonObject>> {
        val request = buildGetRequest("/repos/${encodePath(owner)}/${encodePath(repo)}/issues/$issueNumber/comments?per_page=100")
        return executeRequest(request).map { body ->
            parseJson(body).asJsonArray.map { it.asJsonObject }
        }
    }

    /**
     * List branches for a repository.
     */
    fun getBranches(owner: String, repo: String): Result<List<JsonObject>> {
        val request = buildGetRequest("/repos/${encodePath(owner)}/${encodePath(repo)}/branches?per_page=100")
        return executeRequest(request).map { body ->
            parseJson(body).asJsonArray.map { it.asJsonObject }
        }
    }

    /**
     * List pull requests for a given repository.
     */
    fun getPullRequests(
        owner: String,
        repo: String,
        state: String = "open"
    ): Result<List<JsonObject>> {
        val path = "/repos/${encodePath(owner)}/${encodePath(repo)}/pulls?state=$state&per_page=50"
        val request = buildGetRequest(path)
        return executeRequest(request).map { body ->
            parseJson(body).asJsonArray.map { it.asJsonObject }
        }
    }

    /**
     * Get commits for a pull request.
     */
    fun getPullRequestCommits(owner: String, repo: String, prNumber: Int): Result<List<JsonObject>> {
        val path = "/repos/${encodePath(owner)}/${encodePath(repo)}/pulls/$prNumber/commits?per_page=50"
        val request = buildGetRequest(path)
        return executeRequest(request).map { body ->
            parseJson(body).asJsonArray.map { it.asJsonObject }
        }
    }

    /**
     * Get all labels for a repository.
     */
    fun getLabels(owner: String, repo: String): Result<List<JsonObject>> {
        val path = "/repos/${encodePath(owner)}/${encodePath(repo)}/labels?per_page=100"
        val request = buildGetRequest(path)
        return executeRequest(request).map { body ->
            parseJson(body).asJsonArray.map { it.asJsonObject }
        }
    }

    /**
     * Build a URL to open a specific file on GitHub.
     */
    fun getFileUrl(
        owner: String,
        repo: String,
        filePath: String,
        branch: String,
        lineNumber: Int? = null
    ): String {
        val encodedPath = filePath.replace(" ", "%20")
        val base = "$WEB_BASE_URL/$owner/$repo/blob/$branch/$encodedPath"
        return if (lineNumber != null) "$base#L$lineNumber" else base
    }

    /**
     * Get the clone URL (HTTPS or SSH).
     */
    fun getCloneUrl(owner: String, repo: String, useSsh: Boolean = false): String {
        return if (useSsh) {
            "git@github.com:$owner/$repo.git"
        } else {
            "https://github.com/$owner/$repo.git"
        }
    }

    /**
     * URL-encode a path segment.
     */
    private fun encodePath(segment: String): String =
        URLEncoder.encode(segment, "UTF-8")

    /**
     * Exception class for GitHub API errors.
     */
    class GitHubApiException(
        val statusCode: Int,
        message: String
    ) : Exception(message)
}
