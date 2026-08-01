# 04. Plane separation

| Plane | Owns | Must not do |
|---|---|---|
| Control | UI, API, orchestration, registries, gates, audit | Mount into worker; directly edit targets |
| Execution | ephemeral worktree, adapter, runners, scanners | Read control source; apply/commit/push |
| Artifact | immutable snapshots, patches, results, evidence, hashes | Grant authority or infer approval |

The supervisor serializes a `WorkerRequest`, copies only the approved event slice
to read-only input, creates a detached temporary worktree, and starts a
non-root/network-disabled worker. Output is a typed result plus `patch.diff`.
Governance revalidates every byte independently; worker claims carry no authority.
