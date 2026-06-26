# Issue Mirrors

Use this root for local mirrors of GitHub issues in the fork tracker.

Issue mirrors should stay close enough to the GitHub issue body that an agent can audit drift before changing code.

Required fields for implementation-ready issue mirrors:

- GitHub issue URL or planned issue title
- GitHub milestone
- labels
- issue type when verified through GitHub GraphQL
- source spec or source plan path
- AFK/HITL classification
- target branch
- worktree expectations
- acceptance criteria
- proof oracle

Recommended file path:

`docs/superpowers/issues/<issue-number>-<slug>.md`
