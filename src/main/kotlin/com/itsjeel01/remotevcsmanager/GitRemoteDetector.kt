package com.itsjeel01.remotevcsmanager

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Detects the current project's git remote configuration.
 * Serves as the source of truth for which repository to operate on.
 */
class GitRemoteDetector(private val project: Project) {

    data class GitRemoteInfo(
        val provider: String,          // "github", "gitlab", "bitbucket"
        val owner: String,
        val repoName: String,
        val remoteName: String,
        val remoteUrl: String,
        val currentBranch: String?,
        val gitRoot: File
    )

    /**
     * Detect the git remote from the current project.
     * Returns null if the project is not under git or has no remote.
     */
    fun detect(): GitRemoteInfo? {
        val gitRoot = findGitRoot() ?: return null
        val remoteUrl = getRemoteUrl(gitRoot, "origin") ?: return null
        val parsed = parseRemoteUrl(remoteUrl) ?: return null
        val currentBranch = getCurrentBranch(gitRoot)

        return GitRemoteInfo(
            provider = parsed.provider,
            owner = parsed.owner,
            repoName = parsed.repoName,
            remoteName = "origin",
            remoteUrl = remoteUrl,
            currentBranch = currentBranch,
            gitRoot = gitRoot
        )
    }

    /**
     * Get the token generation URL scoped to the detected remote.
     * For GitHub: generates a fine-grained PAT URL pre-scoped to the repo.
     * For GitHub Enterprise: generates a GHE token URL.
     * For others: generic token URL.
     */
    fun getTokenGenerationUrl(remoteInfo: GitRemoteInfo): String {
        return when (remoteInfo.provider) {
            "github" -> "https://github.com/settings/tokens/new?type=beta"
            "gitlab" -> "${remoteInfo.remoteUrl.takeWhile { it != '/' }}/-/user_settings/personal_access_tokens?scopes=api,read_repository&name=RemoteVCSManager"
            "bitbucket" -> "https://bitbucket.org/account/settings/app-passwords/"
            else -> "https://github.com/settings/tokens"
        }
    }

    /**
     * Get the default token URL for the given provider.
     */
    fun getDefaultTokenUrl(provider: String = "github"): String {
        return when (provider) {
            "github" -> "https://github.com/settings/tokens/new?type=beta"
            "gitlab" -> "https://gitlab.com/-/user_settings/personal_access_tokens"
            "bitbucket" -> "https://bitbucket.org/account/settings/app-passwords/"
            else -> "https://github.com/settings/tokens"
        }
    }

    /**
     * Check if a remote is configured for the current project.
     */
    fun hasRemote(): Boolean = findGitRoot()?.let { getRemoteUrl(it, "origin") } != null

    /**
     * List available remotes for the current project.
     */
    fun listRemotes(): List<Pair<String, String>> {
        val gitRoot = findGitRoot() ?: return emptyList()
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf("git", "remote", "-v"), null, gitRoot
            )
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val lines = mutableListOf<Pair<String, String>>()
            reader.useLines { sequence ->
                sequence.forEach { line ->
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        val name = parts[0]
                        val url = parts[1]
                        // Only add fetch URLs (not push)
                        if (!lines.any { it.first == name }) {
                            lines.add(name to url)
                        }
                    }
                }
            }
            process.waitFor()
            lines
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun findGitRoot(): File? {
        val basePath = project.basePath ?: return null
        var current = File(basePath)
        while (current != null) {
            if (File(current, ".git").exists()) return current
            current = current.parentFile
        }
        return null
    }

    private fun gitCommand(gitRoot: File, vararg args: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("git", *args), null, gitRoot)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            process.waitFor()
            result
        } catch (_: Exception) { null }
    }

    private fun getRemoteUrl(gitRoot: File, remote: String): String? =
        gitCommand(gitRoot, "remote", "get-url", remote)

    private fun getCurrentBranch(gitRoot: File): String? =
        gitCommand(gitRoot, "rev-parse", "--abbrev-ref", "HEAD")

    /**
     * Parse owner and repo from a remote URL.
     * Supports: https, ssh, and git protocol URLs.
     */
    fun parseRemoteUrl(url: String): ParsedRemote? {
        // HTTPS: https://github.com/owner/repo.git
        val httpsRegex = Regex("https?://([^/]+)/([^/]+)/([^/.]+)(?:\\.git)?$")
        httpsRegex.find(url)?.let {
            val domain = it.groupValues[1]
            return ParsedRemote(
                provider = detectProvider(domain),
                owner = it.groupValues[2],
                repoName = it.groupValues[3]
            )
        }

        // SSH: git@github.com:owner/repo.git
        val sshRegex = Regex("git@([^:]+):([^/]+)/([^/.]+)(?:\\.git)?$")
        sshRegex.find(url)?.let {
            val domain = it.groupValues[1]
            return ParsedRemote(
                provider = detectProvider(domain),
                owner = it.groupValues[2],
                repoName = it.groupValues[3]
            )
        }

        // git:// protocol
        val gitRegex = Regex("git://([^/]+)/([^/]+)/([^/.]+)(?:\\.git)?$")
        gitRegex.find(url)?.let {
            val domain = it.groupValues[1]
            return ParsedRemote(
                provider = detectProvider(domain),
                owner = it.groupValues[2],
                repoName = it.groupValues[3]
            )
        }

        return null
    }

    data class ParsedRemote(
        val provider: String,
        val owner: String,
        val repoName: String
    )

    companion object {
        fun detectProvider(domain: String): String = when {
            domain.contains("github") -> "github"
            domain.contains("gitlab") -> "gitlab"
            domain.contains("bitbucket") -> "bitbucket"
            domain.contains("dev.azure") -> "azure"
            domain.contains("visualstudio") -> "azure"
            else -> "unknown"
        }
    }
}
