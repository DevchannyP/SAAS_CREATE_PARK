package io.forgeflow.registry;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public final class EventRegistry {
 public record ScreenEvent(String id,String name,String kind){}
 public record Screen(String id,String name,List<ScreenEvent> events){}
 private final List<Screen> screens=List.of(
  screen("SCR-CONSULT-LIST","농지전용협의내역 조회",e("EVT-01","화면 초기화","QUERY"),e("EVT-02","검색조건 기준정보 조회","QUERY"),e("EVT-03","검색조건 입력·조회 실행","QUERY"),e("EVT-04","조회 결과 상세 이동","NAVIGATION")),
  screen("SCR-CONSULT-REG","농지전용협의 등록",e("EVT-11","등록 화면 초기화","QUERY"),e("EVT-12","입력값 검증","COMMAND"),e("EVT-13","협의 등록","COMMAND")),
  screen("SCR-ISSUE-HISTORY","농지취득 발급내역 현황",e("EVT-21","발급내역 초기 조회","QUERY"),e("EVT-22","발급내역 검색","QUERY"),e("EVT-23","발급내역 출력","EXPORT")),
  screen("SCR-OWNER-SEARCH","농지소유인 조회",e("EVT-31","소유인 조회","QUERY"),e("EVT-32","농지 상세 연결","NAVIGATION")),
  screen("SCR-COMMON-CODE","공통코드 관리",e("EVT-41","코드그룹 조회","QUERY"),e("EVT-42","상세코드 저장","COMMAND"),e("EVT-43","코드 사용중지","COMMAND"))
 );
 private static Screen screen(String id,String name,ScreenEvent...events){return new Screen(id,name,List.of(events));}
 private static ScreenEvent e(String id,String name,String kind){return new ScreenEvent(id,name,kind);}
 public List<Screen> screens(){return screens;}
 public ScreenEvent require(String screenId,String eventId){return screens.stream().filter(s->s.id().equals(screenId)).flatMap(s->s.events().stream().filter(e->e.id().equals(eventId))).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown screen/event pair"));}
}
