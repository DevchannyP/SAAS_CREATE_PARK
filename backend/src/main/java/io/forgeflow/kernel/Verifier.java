package io.forgeflow.kernel;

import java.nio.file.Path;
import java.util.*;

public final class Verifier{
 public enum Check{REPOSITORY_TYPE,PATH_POLICY,BASE_COMMIT,LINKS_AND_SUBMODULES,SCHEMA_TYPECHECK_LINT,
  IMPACTED_TESTS,SECRETS_AND_DEPENDENCIES,DIFF_AND_API_CONTRACT}
 public record CheckResult(Check check,boolean passed,String evidence){}
 public record Report(List<CheckResult> checks,int changedFiles,int changedLines){
  public boolean passed(){return checks.stream().allMatch(CheckResult::passed);}
  public boolean reviewerRequired(TaskContract contract){
   return !passed()||contract.riskLevel().compareTo(TaskContract.RiskLevel.HIGH)>=0
    ||changedFiles>8||changedLines>400
    ||contract.constraints().stream().anyMatch(c->c.matches("(?i).*(auth|permission|payment|migration|api|schema).*"));
  }
 }
 public Report verify(TaskContract contract,String actualBaseSha,Collection<Path> changed,
                      int changedLines,Map<Check,String> deterministicEvidence){
  var results=new ArrayList<CheckResult>();
  results.add(result(Check.REPOSITORY_TYPE,deterministicEvidence));
  try{new PolicyEngine().validateChangedFiles(changed,contract);results.add(new CheckResult(Check.PATH_POLICY,true,"all changed files within writeScopes"));}
  catch(SecurityException e){results.add(new CheckResult(Check.PATH_POLICY,false,e.getMessage()));}
  results.add(new CheckResult(Check.BASE_COMMIT,contract.baseCommitSha().equals(actualBaseSha),
   "expected="+contract.baseCommitSha()+",actual="+actualBaseSha));
  for(Check check:Check.values())if(results.stream().noneMatch(r->r.check()==check))results.add(result(check,deterministicEvidence));
  return new Report(List.copyOf(results),changed.size(),changedLines);
 }
 private static CheckResult result(Check check,Map<Check,String> evidence){
  String value=evidence.get(check);
  return new CheckResult(check,value!=null&&!value.isBlank()&&!value.startsWith("FAIL:"),value==null?"FAIL:missing evidence":value);
 }
}
