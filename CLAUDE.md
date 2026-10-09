@AGENTS.md

## Claude Code specifics

- Shared permissions live in `.claude/settings.json`; personal overrides go in `.claude/settings.local.json` (git-ignored).
- **Plan first for refactors.** For refactors and other multi-file changes, propose a plan and wait for approval before editing. Small, local fixes can go straight ahead.
- **Subagents sparingly.** Do searches and edits inline. Spawn a subagent only when explicitly asked.
- **Concise reports.** Finish with what changed, the test result and any caveats. Do not restate the diff.
- No preferred model; use the session default.
