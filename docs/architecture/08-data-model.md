# 08. Data model

Versioned objects share `version`, `content_hash`, `created_at`, `created_by`,
`source_refs`, and `status`. Core aggregates are Project; fixed Screen/ScreenEvent;
ArchitectureProfile; HarnessDefinition/Version; Thread/Message; DesignSnapshot;
ImplementationQueueItem; WorkLoopRun/Phase/Invocation; PatchBundle; TestRun;
EvaluationRun/Finding; Evidence; TokenLedger; HumanGate; and AuditLog.

TraceLink stores `(source_type, source_id, target_type, target_id, relation_type,
event_id, version)` and is constrained to an event in the same screen. Artifact
content is immutable after publication; new content creates a version. Hashes are
canonical SHA-256. Mutable lifecycle rows use optimistic versions and transactions.

The trace chain is SCREEN → EVENT → REQUIREMENT → MOCKUP_CONTROL → API_OPERATION
→ TABLE.COLUMN → CODE_SYMBOL → TEST_CASE → EVIDENCE.
