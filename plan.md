# Remote VCS Manager - Development Plan

## Overview
A JetBrains IDE plugin for managing remote version control systems. Initially targets Android Studio (all JetBrains IDEs) + GitHub (scalable to other providers).

## Architecture

### Package: `com.itsjeel01.remotevcsmanager`

```
com.itsjeel01.remotevcsmanager/
├── RemoteVcsManagerPlugin.kt          # Plugin lifecycle & project service
├── providers/
│   ├── RemoteVcsProvider.kt           # Provider interface (abstract)
│   ├── github/
│   │   ├── GitHubProvider.kt          # GitHub implementation
│   │   ├── GitHubApiClient.kt         # GitHub REST API v3 client (OkHttp)
│   │   └── GitHubAuth.kt              # PAT storage & validation
├── ui/
│   ├── RemoteVcsToolWindowFactory.kt  # ToolWindowFactory registration
│   ├── RemoteVcsToolWindowPanel.kt    # Main tool window content
│   ├── RemoteVcsTreeCellRenderer.kt   # Custom tree cell renderer
│   ├── RepositorySelectionDialog.kt   # Repository selection dialog
│   ├── Notifications.kt               # Notification utilities
│   └── Icons.kt                       # Icon definitions
├── settings/
│   ├── RemoteVcsSettingsState.kt      # Persistent state (PersistenceStateComponent)
│   ├── RemoteVcsConfigurable.kt       # Settings page (Configurable)
│   └── RemoteVcsSettingsPanel.kt      # Settings form
├── actions/
│   ├── CloneRepositoryAction.kt       # Clone a remote repo
│   ├── CreateRepositoryAction.kt      # Create repo on remote
│   ├── OpenOnRemoteAction.kt          # Open current file on GitHub
│   └── AddRemoteAction.kt             # Add remote to current project
└── models/
    ├── RemoteRepository.kt            # Repo data model
    ├── PullRequest.kt                 # PR data model
    └── RemoteAccount.kt               # Account data model
```

## Phases (Atomic Steps)

### Phase 1: Foundation
- [x] 1.1 Project scaffolding (Gradle, plugin.xml, dependencies)
- [x] 1.2 Create plan.md (this file)
- [x] 1.3 Create settings/state persistence (RemoteVcsSettingsState)
- [x] 1.4 Create provider abstraction (RemoteVcsProvider interface)

### Phase 2: GitHub Integration
- [x] 2.1 GitHub auth model (GitHubAuth - token storage/validation)
- [x] 2.2 GitHub API client (GitHubApiClient - REST endpoints)
- [x] 2.3 GitHub provider impl (GitHubProvider)

### Phase 3: UI
- [x] 3.1 Settings panel & configurable
- [x] 3.2 Tool window factory & panel
- [x] 3.3 Tree renderer & icons

### Phase 4: Actions
- [x] 4.1 Open on Remote action
- [x] 4.2 Clone Repository action
- [x] 4.3 Create Repository action
- [x] 4.4 Add Remote action

### Phase 5: Polish
- [x] 5.1 Error handling & notifications
- [x] 5.2 Plugin.xml finalization
- [x] 5.3 Testing & verification (builds successfully)

## Key Design Decisions
1. **Provider Pattern** - Interface-driven for multi-provider support
2. **OkHttp** for HTTP (already bundled with IntelliJ)
3. **GitHub REST API v3** - simpler than GraphQL for MVP
4. **PAT (Personal Access Token)** auth - simplest auth method
5. **PersistentStateComponent** for settings persistence
6. **ToolWindow** for main UI interaction point
