package io.forgeflow.workloop;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/v1")
public class WorkloopController{
 record Selection(String screenId,String eventId){}
 record RunRequest(@Pattern(regexp="DESIGN|IMPLEMENT")String loopType,String screenId,String eventId){}
 private final WorkloopService service;
 WorkloopController(WorkloopService service){this.service=service;}
 @PostMapping("/design-snapshots/approve") Map<String,String> approve(@RequestBody Selection x){return service.approve(x.screenId,x.eventId);}
 @PostMapping("/design-snapshots/reopen") Map<String,String> reopen(@RequestBody Selection x){return service.reopen(x.screenId,x.eventId);}
 @PostMapping("/runs") Map<String,String> run(@RequestBody RunRequest x){return service.start(x.loopType,x.screenId,x.eventId);}
 @PostMapping("/design-runs") Map<String,String> designRun(@RequestBody Selection x){return service.start("DESIGN",x.screenId,x.eventId);}
 @PostMapping("/implementation-runs") Map<String,String> implementationRun(@RequestBody Selection x){return service.start("IMPLEMENT",x.screenId,x.eventId);}
 @GetMapping("/implementation-queue") java.util.List<java.util.Map<String,Object>> queue(@RequestParam String screenId,@RequestParam(required=false) String eventId){return eventId==null?service.queue(screenId):service.queue(screenId,eventId);}
 @GetMapping("/design-snapshots") java.util.List<java.util.Map<String,Object>> snapshots(@RequestParam String screenId,@RequestParam(required=false)String eventId){return eventId==null?service.snapshots(screenId):service.snapshots(screenId,eventId);}
}
