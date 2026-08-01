# Phase checkpoint

| Phase | State | Checkpoint |
|---|---|---|
| 0 | PASS | baseline, invariants, gap, manifest |
| 1 | PASS | architecture 03–14 |
| 2 | PASS | React build, manifest tests, server-backed trace/approval/run/harness UI |
| 3 | PASS | PostgreSQL 17, Flyway V1/V2, persistent REST state verified |
| 4 | PASS | D00–D10 progression, evidence, snapshot and approval verified |
| 5 | PASS | 15 event SSOTs, queue uniqueness and stale invalidation verified |
| 6 | PASS | C00–C12, PatchBundle, evidence and HUMAN_TEST verified |
| 7 | PASS | path guard tests and fail-closed adapter policy verified |
| 8 | PASS | allowlist, real Git apply check, apply and rollback test verified |
| 9 | PASS | build, unit, integration and hard-gate evidence available |
| 10 | PASS | Compose health, web HTTP and persisted E2E acceptance verified |

Production use of the real Codex adapter and a real external target remains an
explicit deployment decision and is fail-closed when the required executable,
safe flags, mount, approval, or expected Git revision is absent.
