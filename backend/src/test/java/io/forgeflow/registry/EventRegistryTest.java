package io.forgeflow.registry;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class EventRegistryTest{
 @Test void eventManifestIsFixed(){
  var r=new EventRegistry();
  assertEquals(5,r.screens().size());
  assertEquals(15,r.screens().stream().mapToInt(s->s.events().size()).sum());
  assertEquals(15,r.screens().stream().flatMap(s->s.events().stream()).map(EventRegistry.ScreenEvent::id).distinct().count());
  assertThrows(IllegalArgumentException.class,()->r.require("SCR-CONSULT-LIST","EVT-13"));
 }
}
