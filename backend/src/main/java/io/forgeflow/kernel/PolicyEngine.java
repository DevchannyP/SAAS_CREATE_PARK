package io.forgeflow.kernel;

import java.nio.file.*;
import java.util.*;

public final class PolicyEngine{
 private static final Set<String> DENIED_NAMES=Set.of(".git",".github","infra","deployment","secrets");
 public Path authorize(Path workspace,Path requested,TaskContract contract,boolean write)throws Exception{
  if(!requested.isAbsolute()||!requested.normalize().equals(requested))throw new SecurityException("Absolute normalized path required");
  Path root=workspace.toRealPath(LinkOption.NOFOLLOW_LINKS);
  if(Files.isSymbolicLink(requested))throw new SecurityException("Symbolic link rejected");
  Path relative=root.relativize(requested);
  if(relative.toString().isBlank()||relative.startsWith(".."))throw new SecurityException("Outside /workspace");
  for(Path part:relative)if(DENIED_NAMES.contains(part.toString())||part.toString().startsWith(".env"))throw new SecurityException("Denied path");
  if(contract.denyScopes().stream().anyMatch(relative::startsWith))throw new SecurityException("Denied scope");
  List<Path> scopes=write?contract.writeScopes():contract.readScopes();
  if(scopes.stream().noneMatch(relative::startsWith))throw new SecurityException("Outside task scope");
  Path probe=requested;while(!Files.exists(probe,LinkOption.NOFOLLOW_LINKS))probe=probe.getParent();
  if(!probe.toRealPath(LinkOption.NOFOLLOW_LINKS).startsWith(root))throw new SecurityException("External link or mount");
  return requested;
 }
 public void validateChangedFiles(Collection<Path> changed,TaskContract contract){
  for(Path path:changed)if(path.isAbsolute()||contract.writeScopes().stream().noneMatch(path.normalize()::startsWith)
    ||contract.denyScopes().stream().anyMatch(path.normalize()::startsWith))throw new SecurityException("POLICY_VIOLATION:"+path);
 }
}
