package io.forgeflow.harness;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class HarnessRegistryTest {
 @Test void exposesExactlyFiveAgentsPerLoop(){
  var registry=new HarnessRegistry();
  assertEquals(5,registry.list("DESIGN").size());
  assertEquals(5,registry.list("IMPLEMENT").size());
  assertThrows(IllegalArgumentException.class,()->registry.require("DESIGN","coordinator"));
 }
 @Test void visibleFilesRemainFixed(){
  var registry=new HarnessRegistry();
  assertEquals("/harness/design/requirements-agent.md",registry.require("DESIGN","requirements").file());
  assertEquals("/harness/code/review-agent.md",registry.require("IMPLEMENT","review").file());
 }
}
