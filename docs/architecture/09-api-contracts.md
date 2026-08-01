# 09. API contracts

All endpoints are under `/api/v1`, use JSON problem responses, authenticated actor
context, request IDs, optimistic `If-Match` where mutable, and idempotency keys on
commands.

- `/projects`: register/list; path validation rejects overlap with control/run roots,
  non-Git targets, and invalid canonical paths.
- `/screens`, `/screens/{id}/events`: immutable registry.
- `/architecture-profiles`: list/create/apply/delete.
- `/harnesses/{loop}` and `/drafts`: versions, draft, validate, diff, atomic publish.
- `/threads`: create/list/messages; loop type is immutable.
- `/threads/{id}` permits name edits only; any loopType mutation is rejected.
- `/design-runs`, `/design-artifacts`, `/design-snapshots`: run/read/evaluate.
- `/design-snapshots/{id}:approve|reopen`: human approval/version invalidation.
- `/implementation-queue`, `/implementation-runs`: approved event slices only.
- `/runs/{id}`, `/runs/{id}/events`, `:cancel`, `:retry`: status/SSE/control.
- `/patches/{id}`, `/evidence/{id}`: metadata and safe artifact streaming.
- `/patches/{id}`, `/evidence?runId=...`: PatchBundle and evidence metadata; raw
  artifacts require a safe allowlisted artifact endpoint.
- `/governance/validate|dry-run|apply|rollback`: human-authorized operations.
- `/human-gates/{id}`, `/human-gates/{id}:decide`: HUMAN_TEST decision; no
  automated transition to ACCEPTED.
- `/human-gates/{id}:decide`: HUMAN_TEST accept/reject.

Controllers validate transport shape only; application services enforce identity,
authorization, state, transaction, and audit contracts.
