# M1 - Issue Workflow Hardening

## Purpose

Make Anchor's issue workflow reliable for real multi-repository IDE projects. This milestone covers workspace Git root discovery, repository grouping, filtering, sorting, GitHub-authenticated issue loading, detail rendering, and repeated-use ergonomics.

## GitHub Milestone

`M1 - Issue Workflow Hardening`

## Related Specs

- [Issue editor preview design](../specs/2026-06-26-issue-editor-preview-design.md)
- [Tool window navigation refresh design](../specs/2026-06-29-tool-window-navigation-refresh-design.md)

## Related Plans

- [Issue editor preview plan](../plans/2026-06-26-m1-issue-editor-preview-plan.md)
- [Tool window navigation refresh plan](../plans/2026-06-29-m1-tool-window-navigation-refresh-plan.md)

## Related Issues

- [#1 Render GitHub issues in editor preview and group by milestone](../issues/1-render-github-issues-in-editor-preview-and-group-by-milestone.md)
- [#2 Fix issue tool-window header overlap and repo inclusion controls](../issues/2-fix-issue-tool-window-header-overlap-and-repo-inclusion-controls.md)
- [#3 Load GitHub milestones and issue relationships](../issues/3-load-github-milestones-and-issue-relationships.md)
- [#4 Group issue tree by milestones, parent issues, and sub-issues](../issues/4-group-issue-tree-by-milestones-parent-issues-and-sub-issues.md)
- [#5 Wire complete issue tree rendering and Workspace proof](../issues/5-wire-complete-issue-tree-rendering-and-workspace-proof.md)

## Success Criteria

- Anchor discovers relevant workspace GitHub repositories from IDE project roots and remotes.
- The issue tool window groups issues by repository with predictable sorting and filtering.
- Issue detail content renders GitHub Markdown without forcing browser sign-in.
- JCEF rendering and Compose detail rendering have clear ownership and validation paths.
- Targeted tests cover repository discovery and issue grouping behavior.
