package io.forgeflow.git;

import io.forgeflow.kernel.*;
import java.nio.file.Path;
import java.util.*;

public final class GitController{
 public interface GitExecutor{String run(List<String> arguments)throws Exception;}
 public interface CleanCheckoutCi{boolean verify(String commitSha,String branch)throws Exception;}
 public record PublishResult(String commitSha,String branch,String ciStatus){}
 private final GitExecutor git;private final PolicyEngine policy;private final CleanCheckoutCi ci;
 public GitController(GitExecutor git,PolicyEngine policy,CleanCheckoutCi ci){this.git=git;this.policy=policy;this.ci=ci;}
 public PublishResult publish(TaskContract contract,WorkflowController.Outcome outcome,Collection<Path> changed,String branch)throws Exception{
  if(!"VERIFIED".equals(outcome.status())||!outcome.report().passed())
   throw new SecurityException("Commit prohibited: verification failed");
  if(outcome.report().reviewerRequired(contract)&&!outcome.executed().contains(AgentRole.REVIEWER))
   throw new SecurityException("Commit prohibited: reviewer missing");
  if(branch==null||!branch.matches("task/[A-Za-z0-9._-]{1,120}"))
   throw new SecurityException("Only task branches are publishable");
  policy.validateChangedFiles(changed,contract);
  for(Path path:changed)git.run(List.of("add","--",path.toString()));
  git.run(List.of("commit","-m","kernel: "+contract.taskId()));
  String sha=git.run(List.of("rev-parse","HEAD")).trim();
  git.run(List.of("push","origin","HEAD:refs/heads/"+branch));
  if(!ci.verify(sha,branch))throw new IllegalStateException("Clean checkout CI failed; merge prohibited");
  return new PublishResult(sha,branch,"PASS");
 }
 public void rejectAgentGitCommand(List<String> command){
  if(!command.isEmpty()&&Set.of("commit","push","merge","rebase","reset").contains(command.getFirst()))
   throw new SecurityException("Agent Git write prohibited");
 }
}
