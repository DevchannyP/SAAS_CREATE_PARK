package io.forgeflow.project;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
public final class ProjectPathPolicy{
 private final List<Path> protectedRoots;
 public ProjectPathPolicy(List<Path> protectedRoots)throws IOException{this.protectedRoots=protectedRoots.stream().map(p->{try{return p.toRealPath();}catch(IOException e){throw new IllegalArgumentException(e);}}).toList();}
 public Path validate(Path target)throws IOException{
  Path real=target.toRealPath();
  if(protectedRoots.stream().anyMatch(p->real.startsWith(p)||p.startsWith(real)))throw new SecurityException("Project path overlaps protected root");
  if(!Files.isDirectory(real.resolve(".git")))throw new IllegalArgumentException("Target must be a Git worktree");
  return real;
 }
}
