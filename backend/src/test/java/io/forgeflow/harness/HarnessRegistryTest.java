package io.forgeflow.harness;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class HarnessRegistryTest {
 @Test void exposesThreeNonOverlappingAgentsPerLoop(){
  var registry=new HarnessRegistry();
  assertEquals(3,registry.list("DESIGN").size());
  assertEquals(3,registry.list("IMPLEMENT").size());
  assertThrows(IllegalArgumentException.class,()->registry.require("DESIGN","coordinator"));
 }
 @Test void visibleFilesRemainFixed(){
  var registry=new HarnessRegistry();
  assertEquals("/harness/design/requirements-agent.md",registry.require("DESIGN","product-design").file());
  assertEquals("/harness/code/review-agent.md",registry.require("IMPLEMENT","code-review").file());
  assertTrue(registry.require("IMPLEMENT","implementation").content().contains("최소 변경"));
 }
 @Test void assignsTheRightAgentsToLoopPhases(){
  var registry=new HarnessRegistry();
  assertEquals(List.of("product-design"),registry.assigned("DESIGN","D02_REQUIREMENTS").stream().map(HarnessRegistry.Agent::id).toList());
  assertEquals(List.of("test-evidence","code-review"),registry.assigned("IMPLEMENT","C10_REGRESSION").stream().map(HarnessRegistry.Agent::id).toList());
  assertEquals(List.of("code-review"),registry.assigned("IMPLEMENT","C12_HUMAN_TEST").stream().map(HarnessRegistry.Agent::id).toList());
 }
}
