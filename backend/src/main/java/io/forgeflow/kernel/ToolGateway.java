package io.forgeflow.kernel;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ToolGateway{
 public enum Tool{REPO_MAP,SEARCH_SYMBOLS,SEARCH_TEXT,READ_SYMBOL,READ_RANGE,APPLY_PATCH,
  GET_CHANGED_FILES,GET_DIFF,RUN_CHECK,READ_DIAGNOSTIC,REQUEST_CONTEXT,SUBMIT_RESULT}
 public record Output(String text,boolean truncated,Path artifact,int characters){}
 private final Path workspace,artifacts;private final TaskContract contract;private final PolicyEngine policy;
 private final int maxLines,maxCharacters;
 public ToolGateway(Path workspace,Path artifacts,TaskContract contract,PolicyEngine policy,int maxLines,int maxCharacters){
  this.workspace=workspace;this.artifacts=artifacts;this.contract=contract;this.policy=policy;
  this.maxLines=maxLines;this.maxCharacters=maxCharacters;
 }
 public Path authorizeRead(Path absolute)throws Exception{return policy.authorize(workspace,absolute,contract,false);}
 public Path authorizeWrite(Path absolute)throws Exception{return policy.authorize(workspace,absolute,contract,true);}
 public Output bound(Tool tool,String value)throws Exception{
  String[] lines=value.split("\\R",-1);boolean truncated=lines.length>maxLines||value.length()>maxCharacters;
  if(!truncated)return new Output(value,false,null,value.length());
  Files.createDirectories(artifacts);
  Path artifact=artifacts.resolve(tool.name().toLowerCase(Locale.ROOT)+"-"+UUID.randomUUID()+".txt");
  Files.writeString(artifact,value,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
  String preview=String.join(System.lineSeparator(),Arrays.copyOf(lines,Math.min(lines.length,maxLines)));
  if(preview.length()>maxCharacters)preview=preview.substring(0,maxCharacters);
  return new Output(preview,true,artifact,value.length());
 }
}
