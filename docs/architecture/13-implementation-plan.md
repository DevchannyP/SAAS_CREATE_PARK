# 13. Implementation plan

1. Establish React/Spring/PostgreSQL/Compose and shared immutable manifest.
2. Port Soft Orbit regions and locks with UI contract tests.
3. Add Flyway schema and REST persistence.
4. Implement design state machine, harness registry, trace, review, snapshot.
5. Publish event SSOT and approved-only queue with stale invalidation.
6. Implement code plans, adapters, patch/evidence and review routing.
7. Add isolated supervisor and adversarial path/command tests.
8. Add governance validation, dry-run, approved apply and rollback.
9. Aggregate verification evidence and deterministic gates/scores.
10. Package local operations, recovery, telemetry, seed, and human scenarios.

Each phase preserves the mockup hash and runs manifest/static tests. Build claims
require the relevant external tool and are otherwise BLOCKED, never inferred.
