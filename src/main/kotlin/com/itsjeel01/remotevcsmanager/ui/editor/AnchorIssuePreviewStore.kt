package com.itsjeel01.remotevcsmanager.ui.editor

import com.intellij.openapi.Disposable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class AnchorIssuePreviewPayload(
    val title: String,
    val html: String
)

object AnchorIssuePreviewStore {
    private val payloads = ConcurrentHashMap<String, AnchorIssuePreviewPayload>()
    private val listeners = ConcurrentHashMap<String, CopyOnWriteArrayList<(AnchorIssuePreviewPayload) -> Unit>>()

    fun put(file: AnchorIssueVirtualFile, payload: AnchorIssuePreviewPayload): Unit {
        payloads[file.path] = payload
        listeners[file.path]?.forEach { listener -> listener(payload) }
    }

    fun get(file: AnchorIssueVirtualFile): AnchorIssuePreviewPayload? =
        payloads[file.path]

    fun subscribe(file: AnchorIssueVirtualFile, listener: (AnchorIssuePreviewPayload) -> Unit): Disposable {
        val key = file.path
        val fileListeners = listeners.computeIfAbsent(key) { CopyOnWriteArrayList() }
        fileListeners.add(listener)
        return object : Disposable {
            override fun dispose(): Unit {
                fileListeners.remove(listener)
                if (fileListeners.isEmpty()) {
                    listeners.remove(key, fileListeners)
                }
            }
        }
    }

    fun remove(file: AnchorIssueVirtualFile): Unit {
        payloads.remove(file.path)
    }
}
