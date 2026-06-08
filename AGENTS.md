# AGENTS.md — Anchor (Remote VCS Manager)

> Agent guidelines for the Anchor IntelliJ plugin codebase.
> Follow these when modifying any file.

## Brand Identity

- **Name**: Anchor — Remote VCS for JetBrains
- **Tagline**: "Drop anchor. Stay in your IDE."
- **Icon**: Anchor silhouette — circle at top with vertical stem, two diagonal arms, two small circles at bottom. Monochromatic, IDE-theme-aligned (no teal, no purple).
- **Colors**: All UI follows active IDE theme. No hardcoded brand colors in components.

## Architecture

```
com.itsjeel01.remotevcsmanager/
├── actions/           # AnAction subclasses — menu/toolbar actions
├── models/            # Data classes — no logic
├── providers/         # VCS provider abstractions + GitHub impl
│   └── github/
├── settings/          # Persistent config, settings UI
├── ui/
│   ├── components/    # Reusable Compose components
│   ├── detail/        # Issue/PR detail rendering (JCEF + fallback)
│   ├── theme/         # ThemeColors, PlatformFonts, IdeEvents
│   ├── Icons.kt
│   ├── Notifications.kt
│   ├── RemoteVcsToolWindowContent.kt
│   ├── RemoteVcsToolWindowFactory.kt
│   ├── RepositorySelectionDialog.kt
│   ├── CreateIssueDialog.kt
│   └── ToolWindowState.kt
├── GitRemoteDetector.kt
└── RemoteVcsManagerPlugin.kt
```

## Rules

### Kotlin Style
- Multi-line when any line exceeds ~120 chars or has chained calls
- No inline `//` comments explaining what code does — only KDoc on public API
- `val` over `var` unless state mutation is required
- Explicit return types on public functions
- No `!!` — use safe calls or elvis with explicit error

### Compose Patterns
- Use `LocalThemeColors.current` / `LocalPlatformFonts.current` inside `@Composable` — never call `rememberThemeColors()` more than once per screen
- Theme colors are provided at the root via `CompositionLocalProvider`
- All UI components must react to IDE theme changes — use `JBColor`/`JBUI` directly in `@Composable` context, or derive from `ThemeColors`

### Theme System
- `ThemeColors(version)` recomputes ALL colors when IDE theme changes
- `IdeEvents` broadcasts theme/font/scale changes via `UIManager.addPropertyChangeListener`
- NEVER cache theme colors across recompositions — always read fresh

### JCEF / HTML Rendering
- Single JCEF instance per detail view (`VcsDetailHtmlRenderer`)
- Browser creation happens in `SwingPanel.factory` (guaranteed EDT)
- Fallback to Compose rendering if JCEF unavailable
- External links open in system browser; internal anchors stay in JCEF
- HTML sanitization is minimal — only strip JS execution vectors

### File Size
- No file > 400 lines without strong justification
- Extract utility functions to companion/top-level when shared
- Each Compose component in its own file

### Dependencies
- `com.intellij.modules.jcef` declared in plugin.xml
- `sinceBuild = "253"` minimum
- OkHttp for API, Gson for JSON parsing
