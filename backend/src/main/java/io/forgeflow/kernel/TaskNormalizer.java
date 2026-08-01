package io.forgeflow.kernel;

import java.nio.file.Path;
import java.util.*;

public final class TaskNormalizer{
 public TaskContract normalize(String taskId,String goal,List<String> acceptance,List<Path> readScopes,
                               List<Path> writeScopes,String baseCommitSha){
  var deny=List.of(Path.of("generator-ui"),Path.of("generator-api"),Path.of("generator-kernel"),
   Path.of("git-controller"),Path.of("templates"),Path.of("secrets"),Path.of(".git"),Path.of(".github"),
   Path.of(".env"),Path.of("infra"),Path.of("deployment"));
  TaskContract.RiskLevel risk=acceptance.stream().anyMatch(x->x.matches("(?i).*(auth|permission|payment|migration|api|schema).*"))
   ?TaskContract.RiskLevel.HIGH:TaskContract.RiskLevel.LOW;
  return new TaskContract(taskId,goal,acceptance,List.of("preserve public contracts"),readScopes,writeScopes,
   deny,risk,new TaskContract.TokenBudget(8_000,2_000),baseCommitSha);
 }
}
