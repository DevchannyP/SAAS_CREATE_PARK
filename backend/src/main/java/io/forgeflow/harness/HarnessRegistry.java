package io.forgeflow.harness;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public final class HarnessRegistry {
 public record Agent(String id,String name,String file,String content){}
 private final Map<String,List<Agent>> agents=Map.of(
  "DESIGN",List.of(a("requirements","요구사항 명세 Agent","/harness/design/requirements-agent.md"),a("mockup","화면 목업 Agent","/harness/design/mockup-agent.md"),a("data-erd","데이터·ERD Agent","/harness/design/data-erd-agent.md"),a("api-architecture","API·아키텍처 Agent","/harness/design/api-architecture-agent.md"),a("review","설계 검토 Agent","/harness/design/review-agent.md")),
  "IMPLEMENT",List.of(a("frontend","프론트엔드 구현 Agent","/harness/code/frontend-agent.md"),a("backend","백엔드 구현 Agent","/harness/code/backend-agent.md"),a("database","DB·SQL 구현 Agent","/harness/code/database-agent.md"),a("test","테스트 Agent","/harness/code/test-agent.md"),a("review","코드 평가 Agent","/harness/code/review-agent.md")));
 private static Agent a(String id,String name,String file){return new Agent(id,name,file,"# "+name+"\n\nOne Artifact, One Writer\nEvidence Before Score\nHuman Before Final");}
 public List<Agent> list(String loop){if(!agents.containsKey(loop))throw new IllegalArgumentException("Unknown loop type");return agents.get(loop);}
 public Agent require(String loop,String id){return list(loop).stream().filter(a->a.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown harness agent"));}
}
