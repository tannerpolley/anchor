package com.itsjeel01.remotevcsmanager

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import git4idea.repo.GitRepositoryManager
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object GitRootDiscovery {

    fun roots(project: Project): List<File> {
        val projectDir = project.basePath?.let(::File)
        val roots = mutableListOf<File>()
        roots.addAll(repositoryRoots(project))
        roots.addAll(vcsMappingRoots(project, projectDir))
        roots.addAll(moduleContentRoots(project))

        if (projectDir != null) {
            roots.addAll(projectFileMappingRoots(projectDir))
            findGitRoot(projectDir)?.let(roots::add)
        }

        return roots.mapNotNull(::findGitRoot)
            .distinctBy { it.canonicalPath.lowercase() }
            .sortedBy { it.name.lowercase() }
    }

    internal fun projectFileMappingRoots(projectDir: File): List<File> {
        val vcsXml = File(projectDir, ".idea/vcs.xml")
        if (!vcsXml.isFile) return emptyList()

        val document = try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(vcsXml)
        } catch (_: Exception) {
            return emptyList()
        }

        val mappings = document.getElementsByTagName("mapping")
        return (0 until mappings.length).mapNotNull { index ->
            val node = mappings.item(index)
            val attributes = node.attributes ?: return@mapNotNull null
            val vcs = attributes.getNamedItem("vcs")?.nodeValue
            if (vcs != "Git") return@mapNotNull null
            val directory = attributes.getNamedItem("directory")?.nodeValue ?: return@mapNotNull null
            resolveProjectPath(projectDir, directory)
        }.mapNotNull(::findGitRoot)
    }

    internal fun resolveProjectPath(projectDir: File, value: String): File? {
        val path = value.trim()
        if (path.isEmpty() || path == "<Project>") return null

        val filePath = path.removePrefix("file://").let {
            if (Regex("""^/[A-Za-z]:[\\/].*""").matches(it)) it.drop(1) else it
        }

        val resolved = when {
            filePath.startsWith("\$PROJECT_DIR$") -> {
                val relative = filePath.removePrefix("\$PROJECT_DIR$")
                    .trimStart('/', '\\')
                File(projectDir, relative)
            }
            File(filePath).isAbsolute -> File(filePath)
            else -> File(projectDir, filePath)
        }

        return try {
            resolved.canonicalFile
        } catch (_: Exception) {
            resolved.absoluteFile
        }
    }

    internal fun findGitRoot(path: File): File? {
        val start = try {
            path.canonicalFile
        } catch (_: Exception) {
            path.absoluteFile
        }

        return generateSequence(start) { it.parentFile }
            .firstOrNull { File(it, ".git").exists() }
    }

    private fun repositoryRoots(project: Project): List<File> =
        try {
            GitRepositoryManager.getInstance(project).repositories.map { it.root.toNioPath().toFile() }
        } catch (_: Exception) {
            emptyList()
        }

    private fun vcsMappingRoots(project: Project, projectDir: File?): List<File> =
        try {
            ProjectLevelVcsManager.getInstance(project).directoryMappings.mapNotNull { mapping ->
                if (mapping.vcs != "Git") return@mapNotNull null
                val directory = mapping.directory
                if (directory.isBlank() || directory == "<Project>") return@mapNotNull null
                if (projectDir == null) File(directory) else resolveProjectPath(projectDir, directory)
            }
        } catch (_: Exception) {
            emptyList()
        }

    private fun moduleContentRoots(project: Project): List<File> =
        try {
            ModuleManager.getInstance(project).modules.flatMap { module ->
                ModuleRootManager.getInstance(module).contentRoots.map { it.toNioPath().toFile() }
            }
        } catch (_: Exception) {
            emptyList()
        }
}
