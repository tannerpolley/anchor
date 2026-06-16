package com.itsjeel01.remotevcsmanager.providers.github

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.github.authentication.GHAccountsUtil
import org.jetbrains.plugins.github.authentication.accounts.GHAccountManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount

object JetBrainsGithubTokenProvider {

    private val LOG = Logger.getInstance(JetBrainsGithubTokenProvider::class.java)

    fun getToken(project: Project): String? =
        runCatching {
            val account = selectAccount(project) ?: return null
            val accountManager = ApplicationManager.getApplication().getService(GHAccountManager::class.java)
                ?: return null
            runBlocking {
                accountManager.findCredentials(account)
            }?.takeIf { it.isNotBlank() }
        }.onFailure {
            LOG.debug("Unable to read JetBrains GitHub credentials", it)
        }.getOrNull()

    fun getAccountLogins(project: Project): Set<String> =
        runCatching {
            (listOfNotNull(selectAccount(project)) + readAccounts())
                .map { it.name }
                .filter { it.isNotBlank() }
                .map { it.lowercase() }
                .toSet()
        }.onFailure {
            LOG.debug("Unable to read JetBrains GitHub account logins", it)
        }.getOrDefault(emptySet())

    private fun selectAccount(project: Project): GithubAccount? {
        val selected = GHAccountsUtil.getSingleOrDefaultAccount(project)
        if (selected != null) return selected
        val accounts = readAccounts()
        return accounts.firstOrNull { it.server.toString().contains("github.com", ignoreCase = true) }
            ?: accounts.firstOrNull()
    }

    private fun readAccounts(): List<GithubAccount> =
        runCatching {
            val method = GHAccountsUtil::class.java.getMethod("getAccounts")
            val raw = method.invoke(null) as? Set<*> ?: return@runCatching emptyList()
            raw.filterIsInstance<GithubAccount>()
        }.getOrDefault(emptyList())
}
