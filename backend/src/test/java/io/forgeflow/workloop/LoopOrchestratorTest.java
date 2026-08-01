package io.forgeflow.workloop;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import io.forgeflow.registry.EventRegistry;

class LoopOrchestratorTest {
 @Test void designPhasesAdvanceAndEvidenceFailureBlocks(){
  var o=new LoopOrchestrator(new EventRegistry());
  var first=o.start("DESIGN","SCR-CONSULT-LIST","EVT-01");
  assertEquals("D00_SNAPSHOT_FREEZE",first.phase());
  var second=o.advance(first.runId(),true);assertEquals("D01_SCOPE_EVIDENCE",second.phase());
  var blocked=o.advance(first.runId(),false);assertEquals("BLOCKED_EVIDENCE",blocked.status());
 }
 @Test void implementationStartsAtSnapshotVerification(){
  var o=new LoopOrchestrator(new EventRegistry());
  assertEquals("C00_SNAPSHOT_VERIFY",o.start("IMPLEMENT","SCR-COMMON-CODE","EVT-43").phase());
  assertThrows(IllegalArgumentException.class,()->o.start("IMPLEMENT","SCR-COMMON-CODE","EVT-01"));
 }
}
