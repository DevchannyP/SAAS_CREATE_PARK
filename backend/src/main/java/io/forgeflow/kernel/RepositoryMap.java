package io.forgeflow.kernel;

import java.nio.file.Path;
import java.util.*;

public final class RepositoryMap{
 public record Entry(Path path,List<String> symbols,String contentHash){}
 private final Map<Path,Entry> entries=new TreeMap<>();
 public synchronized void applyDiff(Collection<Entry> changed,Collection<Path> deleted){
  deleted.forEach(p->entries.remove(p.normalize()));
  changed.forEach(e->entries.put(e.path().normalize(),e));
 }
 public synchronized List<Entry> select(Collection<Path> scopes){
  return entries.values().stream().filter(e->scopes.stream().anyMatch(s->e.path().startsWith(s))).toList();
 }
}
