# 11. Context and token policy

Context is assembled in stable tiers T0 invariant contract, T1 selection, T2
event design, T3 repository map, T4 necessary source, and T5 failure evidence.
The builder rejects records not matching project/screen/event and allowed symbols.

First runs may include relevant full source; repairs receive diffs, unresolved
findings, and failing spans. Successful logs become hash/summary evidence.
At 80% context utilization old logs are compacted; at 90% a checkpoint starts a
new run. Token, latency, tool output bytes, cache keys, and estimated cost are
recorded per invocation. Budgets fail closed as `BLOCKED_BUDGET`.
