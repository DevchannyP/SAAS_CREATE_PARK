# 02. Gap analysis

| Capability | Baseline | Required implementation |
|---|---|---|
| UI | Single static HTML | React components preserving DOM responsibility and Soft Orbit layout |
| State | Browser localStorage | PostgreSQL plus REST; optimistic recovery in UI |
| Locks | UI conditionals | Server-enforced screen/event/design gates |
| Loops | Log simulation | Deterministic state machine, phases, evidence, retries |
| Harness | Editable local strings | Versioned, validated, atomic server persistence |
| Trace | Embedded arrays | Persistent typed TraceLink graph |
| Worker | None | Separate supervisor, worktree sandbox, real/fake adapters |
| Governance | None | Hash/path/patch verification, dry-run, approved apply/rollback |
| Quality | Claims only | Hard gates before evidence-backed scoring |
| Operations | None | Compose, migrations, health, SSE, audit, redaction |

## Protected paths

- Immutable: the baseline HTML.
- Runtime-invisible: this whole control-plane repository.
- Runtime read-only: copied run input.
- Runtime writable: only the run worktree and output directory.

## Verification limitation

[확인 필요] Compilation, container startup, migrations against PostgreSQL, and
real Codex sandbox behavior cannot be executed until Node, Java, Docker, and Git
are installed and available on PATH. Static contract and path-policy tests remain
possible in the current environment.
