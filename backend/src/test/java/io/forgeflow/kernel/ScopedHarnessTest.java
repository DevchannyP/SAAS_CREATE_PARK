package io.forgeflow.kernel;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class ScopedHarnessTest{
 @TempDir Path temp;
 private TaskContract contract(){
  return new TaskContract(UUID.randomUUID().toString(),"change service",List.of("tests pass"),List.of(),
   List.of(Path.of("src"),Path.of("test")),List.of(Path.of("src")),
   List.of(Path.of(".git"),Path.of(".github"),Path.of(".env"),Path.of("infra")),
   TaskContract.RiskLevel.LOW,new TaskContract.TokenBudget(1000,500),"a".repeat(40));
 }
 @Test void enforcesReadWriteAndDeniedScopes()throws Exception{
  Path workspace=Files.createDirectory(temp.resolve("workspace")).toRealPath();
  Path src=Files.createDirectory(workspace.resolve("src"));Path allowed=Files.createFile(src.resolve("A.java"));
  Path test=Files.createDirectory(workspace.resolve("test"));Path readOnly=Files.createFile(test.resolve("A.test"));
  Path secret=Files.createFile(workspace.resolve(".env"));
  PolicyEngine policy=new PolicyEngine();TaskContract c=contract();
  assertEquals(allowed,policy.authorize(workspace,allowed,c,true));
  assertEquals(readOnly,policy.authorize(workspace,readOnly,c,false));
  assertThrows(SecurityException.class,()->policy.authorize(workspace,readOnly,c,true));
  assertThrows(SecurityException.class,()->policy.authorize(workspace,secret,c,false));
  assertThrows(SecurityException.class,()->policy.authorize(workspace,temp.resolve("outside"),c,false));
 }
 @Test void rejectsSymlinkWhenPlatformSupportsIt()throws Exception{
  Path workspace=Files.createDirectory(temp.resolve("workspace")).toRealPath();
  Files.createDirectory(workspace.resolve("src"));Path outside=Files.createFile(temp.resolve("outside"));
  Path link=workspace.resolve("src/link");
  try{Files.createSymbolicLink(link,outside);}
  catch(UnsupportedOperationException|FileSystemException e){return;}
  assertThrows(SecurityException.class,()->new PolicyEngine().authorize(workspace,link,contract(),false));
 }
 @Test void contextIsScopedAndBudgeted(){
  TaskContract c=contract();var map=new RepositoryMap();
  map.applyDiff(List.of(new RepositoryMap.Entry(Path.of("src/A.java"),List.of("A","run"),"hash"),
   new RepositoryMap.Entry(Path.of("docs/other.md"),List.of(),"hash2")),List.of());
  var selected=map.select(c.readScopes());
  assertEquals(1,selected.size());
  var context=new ContextBuilder().build(c,selected,List.of(),List.of());
  assertTrue(context.stream().anyMatch(i->i.kind().equals("REPOSITORY_MAP")));
  assertFalse(context.toString().contains("docs/other.md"));
 }
 @Test void tokenLedgerFailsClosed(){
  var ledger=new TokenBudgetController(new TaskContract.TokenBudget(10,10));
  ledger.record(AgentRole.WORKER,new TokenBudgetController.Usage(8,2,1,100));
  assertThrows(IllegalStateException.class,()->ledger.record(AgentRole.REVIEWER,new TokenBudgetController.Usage(3,1,1,10)));
 }
 @Test void toolOutputIsBoundedAndStored()throws Exception{
  Path workspace=Files.createDirectory(temp.resolve("workspace"));Files.createDirectory(workspace.resolve("src"));
  var gateway=new ToolGateway(workspace,temp.resolve("artifacts"),contract(),new PolicyEngine(),2,8);
  var output=gateway.bound(ToolGateway.Tool.SEARCH_TEXT,"1234\n5678\n9012");
  assertTrue(output.truncated());assertTrue(Files.exists(output.artifact()));assertTrue(output.text().length()<=8);
 }
}
