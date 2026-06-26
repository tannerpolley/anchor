param(
    [Parameter(Mandatory = $true)]
    [string]$Path,

    [ValidateSet("spec", "plan", "issue")]
    [string]$Kind = "spec"
)

$ErrorActionPreference = "Stop"

$errors = New-Object System.Collections.Generic.List[string]

if (-not (Test-Path -LiteralPath $Path)) {
    $errors.Add("File does not exist: $Path")
} else {
    $content = Get-Content -Raw -LiteralPath $Path
    $lines = $content -split "`r?`n"

    if ($content -notmatch "(?m)^## Decision Ledger\s*$") {
        $errors.Add("Missing required section: ## Decision Ledger")
    } else {
        $sectionStart = -1
        for ($i = 0; $i -lt $lines.Count; $i += 1) {
            if ($lines[$i] -match "^## Decision Ledger\s*$") {
                $sectionStart = $i
                break
            }
        }

        $sectionLines = @()
        for ($i = $sectionStart + 1; $i -lt $lines.Count; $i += 1) {
            if ($lines[$i] -match "^##\s+") {
                break
            }
            $sectionLines += $lines[$i]
        }

        $headerLine = $sectionLines | Where-Object { $_ -match "^\s*\|" } | Select-Object -First 1
        if (-not $headerLine) {
            $errors.Add("Decision ledger table header is missing")
        } else {
            $headers = $headerLine.Trim().Trim("|").Split("|") | ForEach-Object { $_.Trim().ToLowerInvariant() }
            $requiredHeaders = @("decision", "source", "answer", "impact", "deferred?", "risk owner")

            foreach ($requiredHeader in $requiredHeaders) {
                if ($headers -notcontains $requiredHeader) {
                    $errors.Add("Decision ledger missing column: $requiredHeader")
                }
            }

            $dataRows = $sectionLines | Where-Object {
                $_ -match "^\s*\|" -and
                $_ -notmatch "^\s*\|\s*-+" -and
                $_ -ne $headerLine
            }

            if ($dataRows.Count -eq 0) {
                $errors.Add("Decision ledger has no decision rows")
            }

            $deferredIndex = [Array]::IndexOf($headers, "deferred?")
            if ($deferredIndex -ge 0) {
                foreach ($row in $dataRows) {
                    $cells = $row.Trim().Trim("|").Split("|") | ForEach-Object { $_.Trim() }
                    if ($cells.Count -le $deferredIndex) {
                        $errors.Add("Decision row has too few cells: $row")
                    } elseif ($cells[$deferredIndex] -notin @("Yes", "No")) {
                        $errors.Add("Deferred? must be Yes or No: $row")
                    }
                }
            }
        }
    }

    $placeholderPattern = "\b(TBD|TODO|FIXME)\b"
    if ($content -match $placeholderPattern) {
        $errors.Add("Placeholder token found: $($Matches[0])")
    }
}

if ($errors.Count -gt 0) {
    Write-Error (($errors | ForEach-Object { "- $_" }) -join [Environment]::NewLine)
    exit 1
}

Write-Host "Decision ledger valid for $Kind`: $Path"
