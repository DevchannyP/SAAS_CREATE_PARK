package io.forgeflow.workloop;
import io.forgeflow.registry.EventRegistry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public final class LoopOrchestrator {
 public record Phase(String runId,String phase,String status,int iteration){}
 private final EventRegistry registry; private final Map<String,Phase> runs=new ConcurrentHashMap<>();
 LoopOrchestrator(EventRegistry registry){this.registry=registry;}
 public Phase start(String loopType,String screenId,String eventId){registry.require(screenId,eventId);String id=UUID.randomUUID().toString();String phase="DESIGN".equals(loopType)?"D00_SNAPSHOT_FREEZE":"C00_SNAPSHOT_VERIFY";var p=new Phase(id,phase,"RUNNING",0);runs.put(id,p);return p;}
 public Phase advance(String runId,boolean evidencePass){var current=runs.get(runId);if(current==null)throw new IllegalArgumentException("Unknown run");if(!evidencePass){var blocked=new Phase(runId,current.phase(),"BLOCKED_EVIDENCE",current.iteration()+1);runs.put(runId,blocked);return blocked;}String next=switch(current.phase()){case "D00_SNAPSHOT_FREEZE"->"D01_SCOPE_EVIDENCE";case "D01_SCOPE_EVIDENCE"->"D02_REQUIREMENTS";case "D02_REQUIREMENTS"->"D03_ARTIFACTS";case "D03_ARTIFACTS"->"D04_API_ARCHITECTURE";case "D04_API_ARCHITECTURE"->"D05_CROSS_CHECK";case "D05_CROSS_CHECK"->"D06_INDEPENDENT_REVIEW";case "D06_INDEPENDENT_REVIEW"->"D07_MINIMUM_REPAIR";case "D07_MINIMUM_REPAIR"->"D08_TRACE_REGRESSION";case "D08_TRACE_REGRESSION"->"D09_SNAPSHOT";case "D09_SNAPSHOT"->"D10_HUMAN_APPROVAL";case "C00_SNAPSHOT_VERIFY"->"C01_EVENT_CONTEXT";case "C01_EVENT_CONTEXT"->"C02_REPOSITORY_MAP";case "C02_REPOSITORY_MAP"->"C03_IMPLEMENTATION_PLAN";case "C03_IMPLEMENTATION_PLAN"->"C04_VERTICAL_SLICE";case "C04_VERTICAL_SLICE"->"C05_COMPILE";case "C05_COMPILE"->"C06_TEST";case "C06_TEST"->"C07_SECURITY_PERF";case "C07_SECURITY_PERF"->"C08_CODE_REVIEW";case "C08_CODE_REVIEW"->"C09_MINIMUM_REPAIR";case "C09_MINIMUM_REPAIR"->"C10_REGRESSION";case "C10_REGRESSION"->"C11_PATCH_BUNDLE";default->"C12_HUMAN_TEST";};var p=new Phase(runId,next,"RUNNING",current.iteration());runs.put(runId,p);return p;}
}
