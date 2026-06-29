package com.itsjeel01.remotevcsmanager.ui.editor

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileSystem
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class AnchorIssueVirtualFile(
    private val provider: String,
    private val owner: String,
    private val repo: String,
    private val issueNumber: Int,
    title: String
) : VirtualFile() {

    init {
        require(title.isNotBlank()) { "Anchor issue virtual file requires a title" }
    }

    private val content = "# $title\n".toByteArray(StandardCharsets.UTF_8)
    private val modificationStamp = content.contentHashCode().toLong()

    override fun getName(): String = "$repo#$issueNumber.anchor-issue"

    override fun getPresentableName(): String = "$owner/$repo#$issueNumber"

    override fun getFileSystem(): VirtualFileSystem = AnchorIssueVirtualFileSystem

    override fun getPath(): String = "${AnchorIssueVirtualFileSystem.PROTOCOL}://$provider/$owner/$repo/$issueNumber"

    override fun isWritable(): Boolean = false

    override fun isDirectory(): Boolean = false

    override fun isValid(): Boolean = true

    override fun getParent(): VirtualFile? = null

    override fun getChildren(): Array<VirtualFile> = emptyArray()

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream =
        throw UnsupportedOperationException("Anchor issue preview files are read-only")

    override fun contentsToByteArray(): ByteArray = content

    override fun getTimeStamp(): Long = 0L

    override fun getModificationStamp(): Long = modificationStamp

    override fun getLength(): Long = content.size.toLong()

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
        postRunnable?.run()
    }

    override fun getInputStream(): InputStream = ByteArrayInputStream(content)

    override fun getFileType() = PlainTextFileType.INSTANCE
}

object AnchorIssueVirtualFileSystem : VirtualFileSystem() {
    const val PROTOCOL: String = "anchor-issue"

    override fun getProtocol(): String = PROTOCOL

    override fun findFileByPath(path: String): VirtualFile? = null

    override fun refresh(asynchronous: Boolean) = Unit

    override fun refreshAndFindFileByPath(path: String): VirtualFile? = null

    override fun addVirtualFileListener(listener: VirtualFileListener) = Unit

    override fun removeVirtualFileListener(listener: VirtualFileListener) = Unit

    override fun deleteFile(requestor: Any?, vFile: VirtualFile) {
        throw IOException("Anchor issue preview files are read-only")
    }

    override fun moveFile(requestor: Any?, vFile: VirtualFile, newParent: VirtualFile) {
        throw IOException("Anchor issue preview files are read-only")
    }

    override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) {
        throw IOException("Anchor issue preview files are read-only")
    }

    override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): VirtualFile =
        throw IOException("Anchor issue preview files are read-only")

    override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): VirtualFile =
        throw IOException("Anchor issue preview files are read-only")

    override fun copyFile(
        requestor: Any?,
        virtualFile: VirtualFile,
        newParent: VirtualFile,
        copyName: String
    ): VirtualFile =
        throw IOException("Anchor issue preview files are read-only")

    override fun isReadOnly(): Boolean = true
}
