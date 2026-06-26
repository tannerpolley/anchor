package com.itsjeel01.remotevcsmanager.ui.editor

import java.util.concurrent.ConcurrentHashMap

data class AnchorIssuePreviewPayload(
    val title: String,
    val html: String
)

object AnchorIssuePreviewStore {
    private val payloads = ConcurrentHashMap<String, AnchorIssuePreviewPayload>()

    fun put(file: AnchorIssueVirtualFile, payload: AnchorIssuePreviewPayload): Unit {
        payloads[file.path] = payload
    }

    fun get(file: AnchorIssueVirtualFile): AnchorIssuePreviewPayload? =
        payloads[file.path]

    fun remove(file: AnchorIssueVirtualFile): Unit {
        payloads.remove(file.path)
    }
}
