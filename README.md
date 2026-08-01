# ForgeFlow

Production-oriented loop-engineering SaaS scaffold derived from the immutable Soft
Orbit mockup. It separates the React/Spring control plane, PostgreSQL artifacts,
and an ephemeral patch-only runtime worker.

## Local run

Requirements: Docker Compose (or Node 24, Java 21, Maven 3.9, PostgreSQL 17).

1. Set non-default `FORGEFLOW_DB_PASSWORD` and `FORGEFLOW_GATEWAY_TOKEN`
   values. The gateway token must contain at least 16 characters.
2. Run `docker compose up --build`.
3. Open `http://localhost:8080`.
4. Run static invariants with
   `powershell -ExecutionPolicy Bypass -File scripts/verify-static.ps1`.
5. Run the persisted end-to-end acceptance flow with
   `powershell -ExecutionPolicy Bypass -File scripts/verify-e2e.ps1`.
6. Verify every fixed screen/event ownership and approval gate with
   `powershell -ExecutionPolicy Bypass -File scripts/verify-event-matrix.ps1`.

For a release checkpoint, run every gate together:

`powershell -ExecutionPolicy Bypass -File scripts/verify-all.ps1`

The real Codex adapter intentionally fails closed until the supervisor discovers
and validates supported options from the installed `codex --help`. Development
and tests use `FakeCodexWorkerAdapter`.

The end-to-end verifier exercises design phases, evidence persistence, snapshot
approval, implementation phases, PatchBundle creation, HUMAN_TEST approval,
ACCEPTED, and atomic harness publication against the running PostgreSQL stack.

See `docs/architecture/` for gates, isolation, threat model, and acceptance.
