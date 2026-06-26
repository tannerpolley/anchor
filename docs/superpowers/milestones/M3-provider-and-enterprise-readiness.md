# M3 - Provider And Enterprise Readiness

## Purpose

Keep Anchor's GitHub implementation strong while preserving the provider abstraction for future remote VCS providers and GitHub Enterprise installations.

## GitHub Milestone

`M3 - Provider And Enterprise Readiness`

## Related Specs

To add under `docs/superpowers/specs`.

## Related Plans

To add under `docs/superpowers/plans`.

## Related Issues

To add under `docs/superpowers/issues`.

## Success Criteria

- Provider interfaces stay small, testable, and free of GitHub-specific UI leakage.
- GitHub Enterprise base URL behavior is documented and covered by targeted tests.
- Authentication uses JetBrains platform services and has explicit error paths.
- Future provider work can start from a spec without rewriting the GitHub path.
- Public settings and diagnostics make provider/account state understandable inside the IDE.
