# Contributing to Anchor

Thanks for taking the time. Anchor is a solo side project and outside feedback is genuinely the most useful thing at this stage.

---

## Ways to contribute

| Type | Where |
|---|---|
| Bug report | [New issue → Bug report](../../issues/new?template=bug_report.yml) |
| Feature request | [New issue → Feature request](../../issues/new?template=feature_request.yml) |
| Code | Fork → branch → PR |
| Feedback | Comments on any issue |

---

## Development setup

**Requirements:** JDK 17+, IntelliJ IDEA or Android Studio (any recent version)

```bash
# Clone
git clone https://github.com/alph-a07/anchor.git
cd anchor-vcs

# Run the plugin in a sandboxed IDE instance
./gradlew runIde

# Build a distributable ZIP
./gradlew buildPlugin

# Validate plugin.xml and compatibility against the platform range
./gradlew verifyPlugin

# Run tests
./gradlew test
```

The `runIde` task launches IntelliJ Community as the host by default. To test in Android Studio, set `intellijPlatform.type = AI` in `build.gradle.kts`.

---

## Project structure

```
src/main/kotlin/com/itsjeel01/remotevcsmanager/
├── providers/         # Provider abstraction + GitHub implementation
│   ├── RemoteVcsProvider.kt
│   └── github/
│       ├── GitHubApiClient.kt
│       ├── GitHubAuth.kt
│       └── GitHubProvider.kt
├── models/            # Issue, PullRequest, GitBranch, etc.
├── settings/          # Settings panel, state, configurable
└── ui/
    ├── components/    # Reusable UI: StateBadge, LabelChip, BranchPill, etc.
    ├── detail/        # IssueDetailPanel, PullRequestDetailPanel
    └── theme/         # ThemeColors, IdeEvents
```

---

## Pull request guidelines

- One concern per PR — keep them focused
- Include a screenshot or description of what changed visually
- Run `verifyPlugin` before opening the PR — it catches most plugin.xml issues
- Don't add new dependencies without discussion first

#### Branch naming
Create branches from develop using the format type/description (e.g., feat/, fix/, chore/).

#### Commit messages
We enforce [Conventional Commits](https://www.conventionalcommits.org/). PRs with non-compliant branch names or commits will be automatically rejected.

---

## Reporting bugs

The most useful bug reports include:

1. IDE name and version (e.g. Android Studio Meerkat 2024.3.2)
2. Plugin version
3. What you did, what you expected, what happened
4. Any error from **Help → Show Log in Finder/Explorer** (look for `RemoteVCS` or `Anchor`)

---

## Code style

Kotlin standard style. No formatter is enforced yet — just keep it consistent with surrounding code.

---

## License

By contributing, you agree your changes will be licensed under the project's [MIT License](LICENSE).