# Changelog

All notable changes to Anchor — Remote VCS for JetBrains are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versioning follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-beta.4] - 2026-06-11

### Added

- **Issues**
  - Issue list with Open / Closed / All filter chips
  - State badges (green for open, purple for closed)
  - Issue detail panel with full GitHub Flavored Markdown rendering
  - Embedded image rendering inside issue bodies and comments
  - Label chips rendered with GitHub colors
  - Comment thread — view, post, edit, delete own comments
  - Close / Reopen action from detail view
  - Create New Issue dialog with title, body, labels

- **Pull Requests**
  - PR list with Open / Merged / Closed / All filter chips
  - State badges matching GitHub's color system (green open, purple merged, red closed)
  - Source → target branch display on each PR row
  - PR detail panel with full commit history
  - Clickable SHA hashes — opens commit in browser
  - Open in browser action
  - Comment input on PR detail

- **Branches**
  - Full remote branch list with 7-char SHA display
  - Click to open branch in browser

- **Core**
  - Auto-detects active repository from project git remote (HTTPS and SSH)
  - GitHub Personal Access Token authentication via Settings
  - Credentials stored in OS keychain via IntelliJ PasswordSafe
  - GitHub Enterprise base URL override in Settings
  - Auto-refresh on tool window open (configurable)
  - Full GitHub Flavored Markdown: tables, strikethrough, task lists, autolinks, code blocks, blockquotes, GitHub Alerts
  - Respects IDE theme — Dark, Light, Darcula, High Contrast, New UI, Classic UI

---

## [1.0.0-beta.3] - 2026-06-11

### Fixed
- Fixed invalid em dash character in plugin name preventing Marketplace upload.

### Added

- **Issues**
  - Issue list with Open / Closed / All filter chips
  - State badges (green for open, purple for closed)
  - Issue detail panel with full GitHub Flavored Markdown rendering
  - Embedded image rendering inside issue bodies and comments
  - Label chips rendered with GitHub colors
  - Comment thread — view, post, edit, delete own comments
  - Close / Reopen action from detail view
  - Create New Issue dialog with title, body, labels

- **Pull Requests**
  - PR list with Open / Merged / Closed / All filter chips
  - State badges matching GitHub's color system (green open, purple merged, red closed)
  - Source → target branch display on each PR row
  - PR detail panel with full commit history
  - Clickable SHA hashes — opens commit in browser
  - Open in browser action
  - Comment input on PR detail

- **Branches**
  - Full remote branch list with 7-char SHA display
  - Click to open branch in browser

- **Core**
  - Auto-detects active repository from project git remote (HTTPS and SSH)
  - GitHub Personal Access Token authentication via Settings
  - Credentials stored in OS keychain via IntelliJ PasswordSafe
  - GitHub Enterprise base URL override in Settings
  - Auto-refresh on tool window open (configurable)
  - Full GitHub Flavored Markdown: tables, strikethrough, task lists, autolinks, code blocks, blockquotes, GitHub Alerts
  - Respects IDE theme — Dark, Light, Darcula, High Contrast, New UI, Classic UI

---

## [1.0.0-beta.2] - 2026-06-11

### Fixed
- Fixed invalid em dash character in plugin name preventing Marketplace upload.

---

## [1.0.0-beta.1] - 2026-06-09

First public beta. Core GitHub workflow inside any JetBrains IDE.

### Added

- **Issues**
    - Issue list with Open / Closed / All filter chips
    - State badges (green for open, purple for closed)
    - Issue detail panel with full GitHub Flavored Markdown rendering
    - Embedded image rendering inside issue bodies and comments
    - Label chips rendered with GitHub colors
    - Comment thread — view, post, edit, delete own comments
    - Close / Reopen action from detail view
    - Create New Issue dialog with title, body, labels

- **Pull Requests**
    - PR list with Open / Merged / Closed / All filter chips
    - State badges matching GitHub's color system (green open, purple merged, red closed)
    - Source → target branch display on each PR row
    - PR detail panel with full commit history
    - Clickable SHA hashes — opens commit in browser
    - Open in browser action
    - Comment input on PR detail

- **Branches**
    - Full remote branch list with 7-char SHA display
    - Click to open branch in browser

- **Core**
    - Auto-detects active repository from project git remote (HTTPS and SSH)
    - GitHub Personal Access Token authentication via Settings
    - Credentials stored in OS keychain via IntelliJ PasswordSafe
    - GitHub Enterprise base URL override in Settings
    - Auto-refresh on tool window open (configurable)
    - Full GitHub Flavored Markdown: tables, strikethrough, task lists, autolinks, code blocks, blockquotes, GitHub Alerts
    - Respects IDE theme — Dark, Light, Darcula, High Contrast, New UI, Classic UI

---

[1.0.0-beta.1]: https://github.com/alph-a07/anchor/releases/tag/v1.0.0-beta.1
[1.0.0-beta.2]: https://github.com/alph-a07/anchor/releases/tag/v1.0.0-beta.2
[1.0.0-beta.3]: https://github.com/alph-a07/anchor/releases/tag/v1.0.0-beta.3
[1.0.0-beta.4]: https://github.com/alph-a07/anchor/releases/tag/v1.0.0-beta.4