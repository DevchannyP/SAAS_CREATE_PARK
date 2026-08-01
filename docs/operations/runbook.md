# Operations runbook

## Start and health

Set a non-default database password, then run `docker compose up --build`.
Readiness requires PostgreSQL healthy, Flyway successful, API health UP, and web
HTTP 200. The worker runtime is an internal application service with no inbound
controller or separately exposed container.

## Run cancellation and recovery

Cancellation marks the run, sends graceful termination, then force-kills after
the configured grace period. Supervisor cleanup verifies output evidence, hashes
protected roots, removes credentials, and deletes the disposable worktree. It
does not delete the target primary worktree. A retry creates a new run and carries
only the approved snapshot, unresolved findings, and immutable evidence refs.

## Incident response

Any isolation or manifest breach immediately stops the run, records
`FATAL_ISOLATION` or `FATAL_EVENT_DRIFT`, preserves evidence read-only, blocks
patch apply, rotates potentially exposed credentials, and requires human review.
Secrets and personal data are redacted before log persistence.

## Rollback

Governance records the target revision and reverse patch before apply. Rollback
requires a human decision, validates the current revision and reverse patch,
applies atomically in a disposable check worktree, runs mandatory regression, and
only then updates the target. On mismatch it stops without modification.
