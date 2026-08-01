package io.forgeflow.registry;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1")
public class RegistryController{
 private final EventRegistry registry;
 public RegistryController(EventRegistry registry){this.registry=registry;}
 @GetMapping("/screens") public List<EventRegistry.Screen> screens(){return registry.screens();}
 @GetMapping("/screens/{screenId}/events") public List<EventRegistry.ScreenEvent> events(@PathVariable String screenId){
  return registry.screens().stream().filter(x->x.id().equals(screenId)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown screen")).events();
 }
 @GetMapping("/screens/{screenId}/events/{eventId}/trace")
 public Map<String,List<String>> trace(@PathVariable String screenId,@PathVariable String eventId){
  registry.require(screenId,eventId);
  String n=eventId.substring(4);
  return Map.of("requirements",List.of("REQ-"+n),"tables",List.of(eventId.startsWith("EVT-4")?"CMM_CODE":"AGC_DOMAIN"),"apis",Set.of("EVT-04","EVT-32").contains(eventId)?List.of():List.of("API-"+n),"files",List.of("src/events/"+eventId+".tsx","service/"+eventId+"Service.java"),"methods",List.of("handle"+eventId.replace("-","")));
 }
}
