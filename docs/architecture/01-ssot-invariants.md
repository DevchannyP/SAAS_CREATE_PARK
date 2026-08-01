# 01. SSOT invariants

1. The baseline HTML is immutable and its SHA-256 must remain
   `117234D672D24C8E7E0093A0A035445A5B56E41A21DDB662C89D617BF6DCE184`.
2. Screen IDs, event IDs, Korean names, ownership, ordering, and kinds come only
   from `contracts/event-manifest.json`.
3. There are exactly five screens and fifteen events.
4. Screen Context Lock returns no mapping whose screen differs from the selected
   screen. Event Trace Lock returns no trace whose event differs from the selected
   event.
5. Architecture layers retain profile order.
6. Implementation controls remain disabled until an immutable approved design
   snapshot exists for the same project/screen/event/version.
7. Reopening design increments its version and atomically marks queue items and
   patches STALE.
8. Internal phases, runs, activities, and iterations are never stored or exposed
   as business events.
9. Runtime workers cannot mount or address the control-plane repository and only
   return a patch and evidence.
10. HUMAN_TEST is a human gate; scoring never changes state to ACCEPTED.

Any manifest drift is `FATAL_EVENT_DRIFT`; any path/isolation violation is
`FATAL_ISOLATION`.
