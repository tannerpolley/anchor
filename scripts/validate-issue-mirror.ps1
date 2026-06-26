param(
    [Parameter(Mandatory = $true)]
    [string]$IssueFile
)

$ErrorActionPreference = "Stop"
$errors = New-Object System.Collections.Generic.List[string]

if (-not (Test-Path -LiteralPath $IssueFile -PathType Leaf)) {
    $errors.Add("Issue mirror does not exist: $IssueFile")
} else {
    $resolved = (Resolve-Path -LiteralPath $IssueFile).Path
    $repoRoot = (Resolve-Path -LiteralPath ".").Path
    $issueRoot = [IO.Path]::GetFullPath((Join-Path $repoRoot "docs\superpowers\issues"))
    if (-not $resolved.StartsWith($issueRoot, [StringComparison]::OrdinalIgnoreCase)) {
        $errors.Add("Issue mirror must live under docs/superpowers/issues")
    }

    $content = Get-Content -Raw -LiteralPath $IssueFile
    $requiredInline = @(
        "GitHub Issue",
        "GitHub Milestone",
        "Issue Type",
        "Source Plan",
        "Classification",
        "Labels",
        "Goal Command",
        "Execution Mode",
        "Worktree Policy",
        "Integration Policy",
        "TDD Policy",
        "Parallelization Plan",
        "Reviewer Role",
        "Script Gate Mode"
    )

    foreach ($field in $requiredInline) {
        if ($content -notmatch "(?m)^\*\*$([regex]::Escape($field)):\*\* .+") {
            $errors.Add("Missing metadata field: $field")
        }
    }

    foreach ($section in @("Outcome Summary", "Project Merge", "What To Build", "Acceptance Criteria", "Blocked by", "Non-goals", "Proof Oracle")) {
        if ($content -notmatch "(?m)^## $([regex]::Escape($section))\s*$") {
            $errors.Add("Missing section: $section")
        }
    }

    foreach ($field in @("Outcome Source", "Intent", "Target Output", "Owner", "Interface", "Cutover", "Replaced Path", "Acceptance Proof", "Stop Criteria", "Avoid")) {
        if ($content -notmatch "(?m)^\*\*$([regex]::Escape($field)):\*\* .+") {
            $errors.Add("Missing Outcome Summary field: $field")
        }
    }

    foreach ($field in @("Merge Owner", "Merge Gate", "Merge Policy", "Worktree Cleanup Policy", "Orchestrator Wakeup Policy")) {
        if ($content -notmatch "(?m)^\*\*$([regex]::Escape($field)):\*\* .+") {
            $errors.Add("Missing Project Merge field: $field")
        }
    }

    if ($content -notmatch "(?m)^- \[ \] .+") {
        $errors.Add("Acceptance Criteria must include checkbox bullets")
    }

    $sourcePlan = [regex]::Match($content, "(?m)^\*\*Source Plan:\*\* (.+)$").Groups[1].Value.Trim()
    if ($sourcePlan) {
        if (-not (Test-Path -LiteralPath (Join-Path $repoRoot $sourcePlan) -PathType Leaf)) {
            $errors.Add("Source Plan does not exist: $sourcePlan")
        }
    }
}

if ($errors.Count -gt 0) {
    Write-Error (($errors | ForEach-Object { "- $_" }) -join [Environment]::NewLine)
    exit 1
}

Write-Host "Issue mirror valid: $IssueFile"
