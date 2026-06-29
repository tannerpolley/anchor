package com.itsjeel01.remotevcsmanager.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals

class AnchorIssueFileEditorTest {

    @Test
    fun editorOverridesRequiredFileAccessor(): Unit {
        val method = AnchorIssueFileEditor::class.java.getMethod("getFile")

        assertEquals(AnchorIssueFileEditor::class.java, method.declaringClass)
    }
}
