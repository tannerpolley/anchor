package com.itsjeel01.remotevcsmanager

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class GitRootDiscoveryTest {

    @Test
    fun projectFileMappingRootsReturnsSiblingWorkspaceRepos(): Unit {
        val tempDir = Files.createTempDirectory("anchor-workspace").toFile()
        try {
            val workspace = File(tempDir, "Workspace").apply { mkdirs() }
            val ideaDir = File(workspace, ".idea").apply { mkdirs() }
            val epcsaft = File(tempDir, "Engineering/ePC-SAFT").withGitDir()
            val lithium = File(tempDir, "Engineering/Lithium_Extraction").withGitFile()
            val superpowers = File(tempDir, "Projects/superpowers-project").withGitDir()

            File(ideaDir, "vcs.xml").writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <project version="4">
                  <component name="VcsDirectoryMappings">
                    <mapping directory="${'$'}PROJECT_DIR${'$'}/../Engineering/ePC-SAFT" vcs="Git" />
                    <mapping directory="${'$'}PROJECT_DIR${'$'}/../Engineering/Lithium_Extraction" vcs="Git" />
                    <mapping directory="${'$'}PROJECT_DIR${'$'}/../Projects/superpowers-project" vcs="Git" />
                  </component>
                </project>
                """.trimIndent()
            )

            val roots = GitRootDiscovery.projectFileMappingRoots(workspace)
                .map { it.canonicalFile }
                .sortedBy { it.name }

            assertEquals(
                listOf(epcsaft, lithium, superpowers).map { it.canonicalFile }.sortedBy { it.name },
                roots
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun resolveProjectPathExpandsProjectDirMacro(): Unit {
        val workspace = File("C:/workspaces/Workspace")
        val resolved = GitRootDiscovery.resolveProjectPath(
            workspace,
            "${'$'}PROJECT_DIR${'$'}/../Projects/jetbrains-bridge"
        )

        assertEquals(
            File("C:/workspaces/Projects/jetbrains-bridge").canonicalPath,
            resolved?.canonicalPath
        )
    }

    private fun File.withGitDir(): File {
        mkdirs()
        File(this, ".git").mkdir()
        return canonicalFile
    }

    private fun File.withGitFile(): File {
        mkdirs()
        File(this, ".git").writeText("gitdir: ../.git/worktrees/${name}")
        return canonicalFile
    }
}
