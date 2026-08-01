package io.forgeflow.git;

import java.nio.file.*;
import java.util.List;

public final class WorktreeController{
 public interface RepositoryGit{
  String run(Path repository,List<String> arguments)throws Exception;
 }
 private final Path generatedRoot;private final RepositoryGit git;
 public WorktreeController(Path generatedRoot,RepositoryGit git){
  this.generatedRoot=generatedRoot.toAbsolutePath().normalize();this.git=git;
 }
 public Path create(Path targetRepository,String projectId,String runId,String baseCommitSha)throws Exception{
  if(!projectId.matches("[A-Za-z0-9_-]{1,64}")||!runId.matches("[0-9a-fA-F-]{36}")
    ||!baseCommitSha.matches("[0-9a-f]{40}"))throw new SecurityException("Invalid worktree identity");
  Path repository=targetRepository.toRealPath(LinkOption.NOFOLLOW_LINKS);
  if(!Files.exists(repository.resolve(".git"),LinkOption.NOFOLLOW_LINKS))throw new IllegalArgumentException("Target is not Git");
  if(Files.exists(repository.resolve(".gitmodules"),LinkOption.NOFOLLOW_LINKS))throw new SecurityException("Submodules prohibited");
  String actual=git.run(repository,List.of("rev-parse","--verify",baseCommitSha+"^{commit}")).trim();
  if(!actual.equals(baseCommitSha))throw new SecurityException("Base commit mismatch");
  Path worktree=generatedRoot.resolve(projectId).resolve("worktrees").resolve(runId).normalize();
  if(!worktree.startsWith(generatedRoot)||Files.exists(worktree,LinkOption.NOFOLLOW_LINKS))
   throw new SecurityException("Unsafe worktree path");
  Files.createDirectories(worktree.getParent());
  git.run(repository,List.of("worktree","add","--detach",worktree.toString(),baseCommitSha));
  if(!Files.isDirectory(worktree)||!Files.exists(worktree.resolve(".git"),LinkOption.NOFOLLOW_LINKS))
   throw new SecurityException("Git did not create an isolated worktree");
  return worktree.toRealPath(LinkOption.NOFOLLOW_LINKS);
 }
}
