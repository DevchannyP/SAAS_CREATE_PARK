# 14. Acceptance plan

Acceptance evidence is grouped by UI, A loop, B loop, isolation, governance,
operations, and full regression. Every assertion records command, environment,
revision, timestamp, exit code, artifact hash, and log reference. Negative tests
must demonstrate denial plus audit entry and unchanged protected hashes.

Release entry requires all hard gates, real PostgreSQL migrations, frontend and
backend builds, unit/contract/integration/UI tests, real container isolation
attacks, a fake-adapter deterministic suite, and one disposable target project
end-to-end run. A human then performs design approval, patch inspection,
HUMAN_TEST, approved apply, and rollback. No automated suite may create ACCEPTED.
