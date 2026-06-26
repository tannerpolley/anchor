package com.itsjeel01.remotevcsmanager.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFailsWith

class AnchorIssueVirtualFileTest {

    @Test
    fun issueFilePathIsStableForSameIssue(): Unit {
        val first = AnchorIssueVirtualFile("github", "octo", "repo", 42, "Issue title")
        val second = AnchorIssueVirtualFile("github", "octo", "repo", 42, "Issue title")

        assertEquals(first.path, second.path)
        assertEquals("octo/repo#42.md", first.presentableName)
        assertEquals("repo#42.md", first.name)
    }

    @Test
    fun issueFilePathDiffersForDifferentIssues(): Unit {
        val first = AnchorIssueVirtualFile("github", "octo", "repo", 42, "Issue title")
        val second = AnchorIssueVirtualFile("github", "octo", "repo", 43, "Issue title")

        assertNotEquals(first.path, second.path)
    }

    @Test
    fun previewStoreReturnsLatestPayloadForIssueFile(): Unit {
        val file = AnchorIssueVirtualFile("github", "octo", "repo", 42, "Issue title")
        val first = AnchorIssuePreviewPayload(title = "First", html = "<p>first</p>")
        val second = AnchorIssuePreviewPayload(title = "Second", html = "<p>second</p>")

        AnchorIssuePreviewStore.put(file, first)
        AnchorIssuePreviewStore.put(file, second)

        assertEquals(second, AnchorIssuePreviewStore.get(file))
    }

    @Test
    fun previewStoreNotifiesActiveSubscribers(): Unit {
        val file = AnchorIssueVirtualFile("github", "octo", "repo", 44, "Issue title")
        val first = AnchorIssuePreviewPayload(title = "First", html = "<p>first</p>")
        val second = AnchorIssuePreviewPayload(title = "Second", html = "<p>second</p>")
        val third = AnchorIssuePreviewPayload(title = "Third", html = "<p>third</p>")
        val observed = mutableListOf<AnchorIssuePreviewPayload>()

        val subscription = AnchorIssuePreviewStore.subscribe(file) { observed.add(it) }

        AnchorIssuePreviewStore.put(file, first)
        AnchorIssuePreviewStore.put(file, second)
        subscription.dispose()
        AnchorIssuePreviewStore.put(file, third)

        assertEquals(listOf(first, second), observed)
    }

    @Test
    fun issueFileRequiresTitle(): Unit {
        assertFailsWith<IllegalArgumentException> {
            AnchorIssueVirtualFile("github", "octo", "repo", 42, "")
        }
    }
}
