# 03. System context

ForgeFlow is a modular control-plane application supervising isolated project
execution. Users register a target repository, select one fixed screen/event,
produce and approve design artifacts, then request an implementation patch.

Control-plane modules are `project`, `screen`, `event`, `architecture`, `harness`,
`design`, `trace`, `thread`, `workloop`, `context`, `worker`, `patch`,
`evaluation`, `evidence`, `governance`, `humanreview`, and `audit`. PostgreSQL is
the transactional system of record. SSE is the one-way run-log channel.

Trust boundaries are browser/API, API/database, supervisor/sandbox, and
governance/target repository. The browser never invokes tools. Orchestration
creates typed commands, never shell strings. Only Governance can apply a verified
patch after explicit human approval.
