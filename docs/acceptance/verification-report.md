# Verification report — 2026-07-24

## Passing evidence

- Static SSOT and immutable baseline verification: PASS.
- Frontend Vitest: 5/5 PASS across manifest and component integration suites.
- Frontend ESLint zero-warning gate: PASS.
- Frontend npm dependency audit during install: zero known vulnerabilities.
- Frontend TypeScript and Vite production build: PASS.
- Backend Maven/JUnit: 10/10 PASS, including real Git patch apply and rollback.
- PostgreSQL 17 health: healthy.
- Flyway migrations V1 and V2: PASS.
- Spring Actuator health through the web proxy: UP.
- Web entry point: HTTP 200.
- Persisted E2E acceptance script: PASS.
- Cancellation and new-run retry: PASS.
- Negative evidence transition to `B_REPAIR`: PASS.
- Evidence-backed evaluation: PASS, score 91.
- API restart persistence: PASS (`ACCEPTED`, fourteen evidence rows).
- Runtime log audit: no ERROR, exception, or warning.
- Request ID propagation: PASS.
- Missing command idempotency key rejection: PASS.
- Duplicate command rejection: PASS.
- Idempotency persistence across API restart: PASS.
- Successful command response replay with the original resource ID: PASS.
- `X-Idempotent-Replay` response marker: PASS.
- Response replay after API restart: PASS.
- Cross-command idempotency-key reuse rejection: PASS.
- Failed command key cleanup and corrected retry: PASS.
- Audit log trace for run retry: PASS.
- Harness server draft save, reload and diff: PASS.
- Thread stale `If-Match` conflict rejection: PASS.
- Architecture profile application with project version increment: PASS.
- Stale project version rejection: PASS.
- Disposable mounted Git target registration: PASS.
- Governance API real patch apply and rollback against mounted target: PASS.
- Screen-scoped immutable event listing: PASS.
- Versioned design artifact create, evaluate and update: PASS.
- Stale design artifact writer rejection: PASS.
- Dedicated design-run and implementation-run entry points: PASS.
- Design snapshot read endpoint: restored and PostgreSQL-backed.
- Trusted Nginx gateway token injection and API verification: PASS.
- Missing and incorrect gateway tokens rejected with HTTP 403: PASS.
- Correct internal gateway token accepted: PASS.
- CSP, MIME sniffing, frame, referrer and permissions headers: PASS.
- Oversized request rejection at 2 MiB boundary: PASS.
- Server trace rendering and screen navigation component flow: PASS.
- Design approval and run-start component flow: PASS.
- Server-backed harness modal and accessible controls: PASS.
- Visible blocked-state rendering for server policy failures: PASS.
- Full five-screen/fifteen-event ownership matrix: PASS.
- Design and implementation run context lock for all fifteen events: PASS.
- Invalid cross-screen event ownership rejection: PASS.
- Implementation-before-approval rejection: PASS.
- Compose without database and gateway secrets: rejected before startup.
- Public Actuator health endpoint: PASS.
- Public Actuator info, metrics and environment endpoints: HTTP 404.
- Unified `verify-all.ps1` release gate: PASS.
- Unified gate fail-fast behavior when the service is unavailable: PASS.
- Safe `.env.example` for both mandatory secrets: present.

The E2E run verifies five screens, server trace lookup, all D00–D10 design
phases, snapshot approval, the implementation queue, all C00–C12 implementation
phases, fourteen evidence rows including isolated worker output, PatchBundle creation, HUMAN_TEST creation and
human decision, final ACCEPTED state, validated atomic harness publication,
cancel/retry, evidence rejection, evaluation, and restart recovery.
All mutation requests require a tracked actor and persistent idempotency key.
The previous memory-only duplicate design/orchestrator endpoints were removed,
so execution now has one PostgreSQL-backed path.
The obsolete standalone worker image was removed; worker execution is internal
and has no inbound endpoint. Mutable drafts, threads, and project architecture
selection use explicit versions and reject stale writers.

## Commands

```text
docker compose build
docker compose up -d
powershell -ExecutionPolicy Bypass -File scripts/verify-static.ps1
powershell -ExecutionPolicy Bypass -File scripts/verify-e2e.ps1
```

## Operational boundary

The deterministic fake worker is the local acceptance adapter. The real Codex
adapter remains fail-closed and may run only where a separately installed Codex
CLI exposes the required safe flags. A real target must be an explicitly
registered, mounted Git worktree; governance never commits or pushes it.
