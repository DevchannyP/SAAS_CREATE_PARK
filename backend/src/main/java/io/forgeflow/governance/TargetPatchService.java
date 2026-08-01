package io.forgeflow.governance;

import io.forgeflow.worker.PathGuard;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Applies a previously validated patch transactionally; never commits or pushes. */
public final class TargetPatchService {
 public record ApplyResult(String revision, String rollbackToken, List<String> changedFiles) {}
 private final Path targetRoot;
 private final Path backupRoot;
 public TargetPatchService(Path targetRoot, Path backupRoot) throws IOException {
  this.targetRoot=targetRoot.toRealPath(); this.backupRoot=backupRoot.toAbsolutePath().normalize(); Files.createDirectories(this.backupRoot);
  if(!Files.isDirectory(this.targetRoot.resolve(".git"))) throw new IllegalArgumentException("Target must be a Git worktree");
 }
 public ApplyResult apply(String unifiedDiff, Set<Path> allowedPaths, String expectedRevision) throws Exception {
  if(unifiedDiff==null||unifiedDiff.isBlank()) throw new IllegalArgumentException("Empty patch");
  var validator=new PatchValidator(); var paths=validator.validate(unifiedDiff,allowedPaths);
  if(!paths.valid()) throw new SecurityException(String.join(",",paths.violations()));
  if(!currentRevision().equals(expectedRevision)) throw new IllegalStateException("Target revision changed");
  String token=UUID.randomUUID().toString(); Path backup=backupRoot.resolve(token); Files.createDirectories(backup);
  var changed=changedFiles(unifiedDiff);
  for(Path relative:changed){Path file=new PathGuard(List.of(targetRoot)).requireAllowed(targetRoot.resolve(relative));if(Files.exists(file)) {Path saved=backup.resolve(relative.toString());Files.createDirectories(saved.getParent());Files.copy(file,saved,StandardCopyOption.COPY_ATTRIBUTES);}}
  runGit("apply","--check","--whitespace=error","-").write(unifiedDiff);
  try { runGit("apply","--whitespace=error","-").write(unifiedDiff); }
  catch(Exception failure){ restore(backup,changed); throw failure; }
  return new ApplyResult(currentRevision(),token,changed.stream().map(Path::toString).toList());
 }
 public void rollback(String token,List<String> changedFiles) throws IOException {
  Path backup=backupRoot.resolve(token).normalize();if(!backup.startsWith(backupRoot)||!Files.isDirectory(backup))throw new SecurityException("Invalid rollback token");
  for(String raw:changedFiles){Path relative=Path.of(raw).normalize();if(relative.isAbsolute()||raw.contains(".."))throw new SecurityException("Invalid rollback path");Path target=targetRoot.resolve(relative).normalize();if(!target.startsWith(targetRoot))throw new SecurityException("Outside target");Path saved=backup.resolve(relative);if(Files.exists(saved)){Files.createDirectories(target.getParent());Files.copy(saved,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.COPY_ATTRIBUTES);}else Files.deleteIfExists(target);}
 }
 private void restore(Path backup,List<Path> changed)throws IOException{rollback(backup.getFileName().toString(),changed.stream().map(Path::toString).toList());}
 private List<Path> changedFiles(String diff){return diff.lines().filter(x->x.startsWith("+++ ")).map(x->x.substring(4).replaceFirst("^b/","").trim()).filter(x->!x.equals("/dev/null")).map(Path::of).toList();}
 private String currentRevision() throws Exception{return runGit("rev-parse","HEAD").output().trim();}
 private GitRun runGit(String...args){return new GitRun(args);}
 private final class GitRun {private final String[] args;GitRun(String[] args){this.args=args;}GitRun write(String input)throws Exception{Process p=process();p.getOutputStream().write(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));p.getOutputStream().close();if(!p.waitFor(30,TimeUnit.SECONDS)){p.destroyForcibly();throw new IllegalStateException("git timeout");}if(p.exitValue()!=0)throw new IllegalStateException("git apply failed");return this;}String output()throws Exception{Process p=process();if(!p.waitFor(30,TimeUnit.SECONDS)){p.destroyForcibly();throw new IllegalStateException("git timeout");}if(p.exitValue()!=0)throw new IllegalStateException("git command failed");return new String(p.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}Process process()throws IOException{var c=new ArrayList<String>();c.add("git");c.addAll(List.of(args));return new ProcessBuilder(c).directory(targetRoot.toFile()).redirectErrorStream(true).start();}}
}
