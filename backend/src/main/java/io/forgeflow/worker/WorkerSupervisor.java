package io.forgeflow.worker;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import io.forgeflow.kernel.*;
public final class WorkerSupervisor{
 private final Path controlRoot,runRoot;private final WorkerAdapter adapter;
 public WorkerSupervisor(Path controlRoot,Path runRoot,WorkerAdapter adapter){this.controlRoot=controlRoot.toAbsolutePath().normalize();this.runRoot=runRoot.toAbsolutePath().normalize();this.adapter=adapter;}
 public WorkerAdapter.Result execute(TaskContract contract,AgentRole role,List<AgentEnvelope.ContextItem> context)throws Exception{
  String runId=contract.taskId();
  if(!runId.matches("[0-9a-fA-F-]{36}"))throw new SecurityException("Invalid run id");
  Path run=runRoot.resolve("runs").resolve(runId).normalize();
  Path worktree=runRoot.resolve("generated-projects").resolve("default").resolve("worktrees").resolve(runId).normalize();
  if(!run.startsWith(runRoot)||!worktree.startsWith(runRoot)||run.startsWith(controlRoot)||worktree.startsWith(controlRoot))
   throw new SecurityException("FATAL_ISOLATION");
  Path input=run.resolve("input"),output=run.resolve("output");
  new PathGuard(List.of(worktree,output)).requireAllowed(worktree.toRealPath());
  if(Files.isWritable(input))throw new SecurityException("Input must be read-only");
  return adapter.execute(new WorkerAdapter.Request(runId,role,worktree,input,output,contract,List.copyOf(context),Duration.ofMinutes(15)));
 }
}
