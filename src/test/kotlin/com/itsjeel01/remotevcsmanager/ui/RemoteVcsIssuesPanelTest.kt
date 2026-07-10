package com.itsjeel01.remotevcsmanager.ui

import com.itsjeel01.remotevcsmanager.GitRemoteDetector.GitRemoteInfo
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RemoteVcsIssuesPanelTest {

    @Test
    fun `creates target for GitHub HTTPS remote`() {
        val root = File("workspace/anchor").absoluteFile

        val target = RemoteVcsIssuesPanel.createTarget(
            remote(
                root = root,
                remoteUrl = "https://github.com/tannerpolley/anchor.git"
            )
        )

        assertEquals(
            RepoIssueTarget(
                displayName = "anchor",
                owner = "tannerpolley",
                repoName = "anchor",
                rootPath = root.absolutePath,
                issuesUrl = "https://github.com/tannerpolley/anchor/issues"
            ),
            target
        )
    }

    @Test
    fun `uses SSH host for GitHub target`() {
        val target = RemoteVcsIssuesPanel.createTarget(
            remote(remoteUrl = "git@github.example.com:tannerpolley/anchor.git")
        )

        assertEquals("https://github.example.com/tannerpolley/anchor/issues", target?.issuesUrl)
    }

    @Test
    fun `uses git protocol host for GitHub target`() {
        val target = RemoteVcsIssuesPanel.createTarget(
            remote(remoteUrl = "git://github.example.com/tannerpolley/anchor.git")
        )

        assertEquals("https://github.example.com/tannerpolley/anchor/issues", target?.issuesUrl)
    }

    @Test
    fun `rejects non GitHub remote`() {
        val target = RemoteVcsIssuesPanel.createTarget(
            remote(
                provider = "gitlab",
                remoteUrl = "https://gitlab.com/tannerpolley/anchor.git"
            )
        )

        assertNull(target)
    }

    private fun remote(
        root: File = File("workspace/anchor").absoluteFile,
        provider: String = "github",
        remoteUrl: String
    ): GitRemoteInfo =
        GitRemoteInfo(
            provider = provider,
            owner = "tannerpolley",
            repoName = "anchor",
            remoteName = "origin",
            remoteUrl = remoteUrl,
            currentBranch = "main",
            gitRoot = root
        )
}
