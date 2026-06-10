package com.itsjeel01.remotevcsmanager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itsjeel01.remotevcsmanager.models.IssueState
import com.itsjeel01.remotevcsmanager.models.PRState
import com.itsjeel01.remotevcsmanager.ui.theme.LocalPlatformFonts
import com.itsjeel01.remotevcsmanager.ui.theme.LocalThemeColors

/**
 * Pill-shaped state badge — Compose component.
 * Uses platform-derived font sizes and theme-aware accent colors.
 */
@Composable
fun StateBadge(issueState: IssueState? = null, prState: PRState? = null, modifier: Modifier = Modifier) {
    val theme = LocalThemeColors.current
    val fs = LocalPlatformFonts.current
    val (text, color) = when {
        issueState == IssueState.OPEN || prState == PRState.OPEN -> "● Open" to theme.GitHub.open
        issueState == IssueState.CLOSED -> "● Closed" to theme.GitHub.closed
        prState == PRState.CLOSED -> "● Closed" to theme.GitHub.closedPr
        prState == PRState.MERGED -> "● Merged" to theme.GitHub.closed
        else -> return
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
        Text(text, color = color, fontSize = fs.xsmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable fun StateBadgeForIssue(issueState: IssueState) = StateBadge(issueState = issueState)
@Composable fun StateBadgeForPR(prState: PRState) = StateBadge(prState = prState)
