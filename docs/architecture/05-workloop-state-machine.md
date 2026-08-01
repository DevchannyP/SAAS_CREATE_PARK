# 05. Workloop state machine

Happy path:

`A_DRAFT → A_REVIEW → A_APPROVED → IMPLEMENTATION_READY → B_RUNNING →
HUMAN_TEST → ACCEPTED`

Repairs and exits:

- `B_RUNNING → B_REPAIR → B_RUNNING`
- `B_RUNNING → RETURN_TO_DESIGN → A_DRAFT`
- `A_APPROVED → A_DRAFT`
- any design version change makes implementation artifacts `STALE`
- missing evidence/budget: `BLOCKED_EVIDENCE` / `BLOCKED_BUDGET`
- manifest/isolation breach: `FATAL_EVENT_DRIFT` / `FATAL_ISOLATION`

Transitions use compare-and-set on state and version in one transaction. ACCEPTED
requires a HumanGate decision. Maximum normal/absolute iterations are 3/5;
repeated defects and no-progress counters are persisted.
