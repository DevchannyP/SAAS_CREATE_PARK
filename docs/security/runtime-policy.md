# Runtime worker policy

Browser traffic reaches the API only through the Nginx gateway. Nginx injects a
deployment-specific `X-ForgeFlow-Gateway` value; the API compares it in constant
time and rejects missing or incorrect values. The API service has no host port.
Deployments must replace both default database and gateway secrets.

The observed local CLI is `codex-cli 0.145.0`. Its help confirms `exec`,
`--strict-config`, `--sandbox workspace-write`, `--ask-for-approval never`,
`--cd`, and feature disabling. The adapter checks help evidence before using each
option and never selects `danger-full-access` or either dangerous bypass option.

Application-level options complement, but do not replace, the container boundary:
the production supervisor must start the worker with network `none`, read-only
root filesystem, dropped capabilities, non-root user, no Docker socket, only the
run worktree/output writable, input read-only, and no control/target mounts.
`approval=never` is fail-closed within workspace-write: denied escalation becomes
a worker failure and cannot expand authority.
