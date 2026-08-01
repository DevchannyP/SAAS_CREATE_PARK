# 07. Runtime isolation

Per run:

- `RUN_ROOT/<run>/worktree`: only code write root
- `RUN_ROOT/<run>/input`: read-only approved event slice
- `RUN_ROOT/<run>/output`: only evidence/patch write root
- fresh empty HOME and allowlisted environment

The control root is never mounted. The target's primary worktree is read-only to
the supervisor and invisible to the worker. Every path passes lexical normalize,
absolute resolution, realpath, containment-by-components, and reparse-point
inspection before and after execution. Windows drives/UNC, alternate streams,
symlinks, junctions, and cross-run roots are rejected.

The container is non-root, drops all capabilities, has read-only rootfs, no
Docker socket, no network, PID/memory/CPU/time limits, and no inherited secrets.
Commands are typed executable/argument records checked against policy; commit,
merge, push, `git -C`, interpreters, and shell expansion are denied. The real
Codex adapter must discover `codex --help`; unsupported flags fail closed.
