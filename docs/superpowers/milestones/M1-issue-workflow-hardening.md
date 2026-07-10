# M1 - Issue Workflow Hardening

## Purpose

Make Anchor's issue workflow reliable for the current project repository. This milestone covers single-repository detection, sorting, milestone and parent/sub-issue grouping, GitHub-authenticated loading, rendered editor previews, and repeated-use ergonomics.

## GitHub Milestone

`M1 - Issue Workflow Hardening`

## Related Specs

- [Issue editor preview design](../specs/2026-06-26-issue-editor-preview-design.md)
- [Single-repository issue navigator design](../specs/2026-07-10-single-repository-issue-navigator-design.md)

## Related Plans

- [Issue editor preview plan](../plans/2026-06-26-m1-issue-editor-preview-plan.md)
- [Single-repository issue navigator plan](../plans/2026-07-10-m1-single-repository-issue-navigator-plan.md)

## Open Issues

- [#1 Render GitHub issues in editor preview and group by milestone](../issues/1-render-github-issues-in-editor-preview-and-group-by-milestone.md)

## Success Criteria

- Anchor detects the current project's GitHub repository from its base Git root and origin remote.
- The issue tool window groups the current repository's issues by milestone and parent/sub-issue relationships.
- Issue detail content renders GitHub Markdown without forcing browser sign-in.
- JCEF rendering and Compose detail rendering have clear ownership and validation paths.
- Targeted tests cover primary-repository detection, issue grouping, and editor-preview behavior.
