# Anchor — Remote VCS for JetBrains

**"Drop anchor. Stay in your IDE."**

Browse GitHub issues, pull requests, and branches without leaving your editor.
Theme-aware rendering that matches your IDE — light or dark, it just works.

## Features

- **Issues, PRs & Branches** — browse, filter, and manage all three with native IDE tool window
- **GitHub-native rendering** — descriptions and comments render using GitHub's own markdown API, so they look exactly like they do on github.com
- **Timeline view** — PR commits appear inline in a chronological timeline with author and timestamp
- **Theme-aware** — all HTML content adapts to your active IDE theme. Switch from light to dark and every element follows
- **Single-click checkout** — switch branches from the tool window using IntelliJ's native git infrastructure
- **Create issues** — full form with label chips, assignee field, and markdown description
- **Open on GitHub** — right-click any file to view it on GitHub at the current line

## Installation

### From JetBrains Marketplace
1. Open your IDE → **Settings** → **Plugins** → **Marketplace**
2. Search for **"Anchor — Remote VCS"**
3. Click **Install**
4. Set up a GitHub personal access token in **Settings** → **Tools** → **Anchor — Remote VCS**

### Manual installation
```bash
git clone https://github.com/itsjeel01/remoteVcsManager.git
cd remoteVcsManager
./gradlew buildPlugin
# Install from build/distributions/ via Settings → Plugins → ⚙ → Install from Disk
```

## Usage

1. Open a project with a GitHub remote configured
2. Open the **Remote VCS** tool window (right sidebar)
3. If this is your first time, set your GitHub PAT in **Settings → Tools → Anchor — Remote VCS**
4. The tool window will auto-detect your remote and load all issues, PRs, and branches

**Keyboard shortcuts** (when focused in the tool window):
- `R` — refresh data
- `Esc` — go back from detail view

## Contributing

Issues, PRs, and ideas are welcome — this is an early-stage project and your input shapes it.

- **Found a bug?** [Open an issue](../../issues/new?template=bug_report.md)
- **Have a feature request?** [Open an issue](../../issues/new?template=feature_request.md)
- **Want to contribute code?** Fork, branch, and PR — see conventions below

### Commit conventions
- `feat:` — new features
- `fix:` — bug fixes
- `chore:` — build, deps, cleanup
- `docs:` — documentation
- `refactor:` — code restructuring
- Commits should be atomic — one logical change per commit

### Branch conventions
- `main` — stable, release-ready
- `develop` — integration branch for features
- `feat/*` — feature branches (e.g., `feat/gitlab-support`)
- `fix/*` — bug fix branches
- Branch from `develop`, PR back to `develop`

## Tech stack

- Kotlin + Compose Multiplatform for UI
- Chromium Embedded Framework (JCEF) for HTML rendering
- GitHub REST API v3 + GFM markdown rendering
- OkHttp for networking, Gson for JSON parsing

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.
