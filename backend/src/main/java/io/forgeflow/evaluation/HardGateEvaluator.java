package io.forgeflow.evaluation;
import java.util.*;

public final class HardGateEvaluator {
 public record Input(boolean manifestMatch,boolean baselineHashMatch,boolean screenLock,boolean eventTrace,boolean approvedSnapshot,boolean designVersionMatch,boolean isolationClean,boolean scopeClean,boolean buildPass,boolean testsPass,boolean contractClean,boolean criticalHighZero,boolean traceComplete,boolean evidencePresent){}
 public record Result(boolean passed,List<String> failures,OptionalInt score){
  public static Result blocked(List<String> failures){return new Result(false,List.copyOf(failures),OptionalInt.empty());}
 }
 public Result evaluate(Input input){
  var failures=new ArrayList<String>();
  if(!input.manifestMatch())failures.add("EVENT_MANIFEST_MISMATCH");if(!input.baselineHashMatch())failures.add("BASELINE_HASH_MISMATCH");if(!input.screenLock())failures.add("SCREEN_CONTEXT_VIOLATION");if(!input.eventTrace())failures.add("EVENT_TRACE_VIOLATION");if(!input.approvedSnapshot())failures.add("DESIGN_NOT_APPROVED");if(!input.designVersionMatch())failures.add("DESIGN_VERSION_MISMATCH");if(!input.isolationClean())failures.add("ISOLATION_VIOLATION");if(!input.scopeClean())failures.add("SCOPE_VIOLATION");if(!input.buildPass())failures.add("BUILD_FAILED");if(!input.testsPass())failures.add("REQUIRED_TEST_FAILED");if(!input.contractClean())failures.add("CONTRACT_MISMATCH");if(!input.criticalHighZero())failures.add("CRITICAL_HIGH_FINDINGS");if(!input.traceComplete())failures.add("TRACE_INCOMPLETE");if(!input.evidencePresent())failures.add("EVIDENCE_MISSING");
  if(!failures.isEmpty())return Result.blocked(failures);return new Result(true,List.of(),OptionalInt.of(99));
 }
}
