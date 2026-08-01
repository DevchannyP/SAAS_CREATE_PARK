package io.forgeflow.worker;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class PathGuard{
 private final List<Path> roots;
 public PathGuard(Collection<Path> allowedRoots)throws IOException{
  roots=allowedRoots.stream().map(p->{try{return p.toRealPath(LinkOption.NOFOLLOW_LINKS);}catch(IOException e){throw new IllegalArgumentException("Invalid allowed root",e);}}).toList();
 }
 public Path requireAllowed(Path candidate)throws IOException{
  if(!candidate.isAbsolute())throw new SecurityException("Absolute normalized path required");
  Path normalized=candidate.normalize();
  if(!normalized.equals(candidate))throw new SecurityException("Traversal rejected");
  Path current=normalized.getRoot();
  for(Path part:normalized){current=current.resolve(part);if(Files.exists(current,LinkOption.NOFOLLOW_LINKS)&&Files.isSymbolicLink(current))throw new SecurityException("Symbolic link rejected");}
  Path probe=normalized;
  while(!Files.exists(probe,LinkOption.NOFOLLOW_LINKS)&&probe.getParent()!=null) probe=probe.getParent();
  Path real=probe.toRealPath(LinkOption.NOFOLLOW_LINKS);
  if(probe.getNameCount()<normalized.getNameCount())real=real.resolve(normalized.subpath(probe.getNameCount(),normalized.getNameCount()));
  boolean allowed=roots.stream().anyMatch(real::startsWith);
  if(!allowed)throw new SecurityException("Path outside run roots");
  return real;
 }
}
