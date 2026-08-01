package io.forgeflow.worker;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public record DockerSandboxSpec(Path hostWorktree,String containerPath,boolean networkDisabled,
                                boolean readOnlyRoot,List<String> droppedCapabilities,
                                Duration timeout,long memoryBytes,double cpus,int pids){
 public DockerSandboxSpec{
  if(!"/workspace".equals(containerPath)||!networkDisabled||!readOnlyRoot
    ||!droppedCapabilities.equals(List.of("ALL")))throw new IllegalArgumentException("Unsafe sandbox spec");
  if(hostWorktree==null||!hostWorktree.toString().replace('\\','/').matches(".*/generated-projects/[^/]+/worktrees/[^/]+$"))
   throw new IllegalArgumentException("Worktree must use generated-projects/{projectId}/worktrees/{runId}");
 }
}
