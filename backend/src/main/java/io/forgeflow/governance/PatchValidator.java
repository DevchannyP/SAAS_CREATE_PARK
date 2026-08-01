package io.forgeflow.governance;
import java.nio.file.*;
import java.util.*;
public final class PatchValidator{
 public record Result(boolean valid,List<String> violations){}
 public Result validate(String unifiedDiff,Set<Path> allowedRelativePaths){
  var violations=new ArrayList<String>();
  for(String line:unifiedDiff.split("\\R")){
   if(line.startsWith("+++ ")||line.startsWith("--- ")){
    String raw=line.substring(4).trim().replaceFirst("^[ab]/","");
    if("/dev/null".equals(raw))continue;
    Path p=Path.of(raw).normalize();
    if(p.isAbsolute()||raw.contains("..")||!allowedRelativePaths.contains(p))violations.add("PROHIBITED_PATH:"+raw);
   }
  }
  return new Result(violations.isEmpty(),List.copyOf(violations));
 }
}
