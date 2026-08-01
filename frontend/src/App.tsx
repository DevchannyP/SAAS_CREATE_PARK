import{useEffect,useMemo,useState}from"react";
import{screens}from"./manifest";
import type{LoopType,TraceSummary}from"./types";
import{api,type Evidence,type HarnessAgent,type HumanGate,type Project,type RunStatus}from"./api";

const empty:TraceSummary={requirements:[],tables:[],apis:[],files:[],methods:[]};

export function App(){
 const[screenId,setScreen]=useState(screens[0].id),[eventId,setEvent]=useState(screens[0].events[0].id);
 const[loop,setLoop]=useState<LoopType>("DESIGN"),[approved,setApproved]=useState(false);
 const[trace,setTrace]=useState<TraceSummary>(empty),[run,setRun]=useState<RunStatus|null>(null);
 const[projects,setProjects]=useState<Project[]>([]),[evidence,setEvidence]=useState<Evidence[]>([]),[gate,setGate]=useState<HumanGate|null>(null);
 const[harnessType,setHarnessType]=useState<"design"|"code"|null>(null),[agents,setAgents]=useState<HarnessAgent[]>([]),[agent,setAgent]=useState(0);
 const[drafts,setDrafts]=useState<Record<string,string>>({}),[draftVersions,setDraftVersions]=useState<Record<string,number>>({}),[logs,setLogs]=useState<string[]>([]),[error,setError]=useState("");
 const screen=useMemo(()=>screens.find(x=>x.id===screenId)!,[screenId]);
 const event=useMemo(()=>screen.events.find(x=>x.id===eventId)!,[screen,eventId]);
 const report=(message:string)=>setLogs(v=>[...v.slice(-11),message]);
 const fail=(e:unknown)=>setError(e instanceof Error?e.message:"요청 처리에 실패했습니다.");
 useEffect(()=>{api.trace(screenId,eventId).then(setTrace).catch(fail)},[screenId,eventId]);
 useEffect(()=>{api.projects().then(setProjects).catch(fail)},[]);
 useEffect(()=>{if(!run?.runId)return;const timer=setInterval(()=>api.runStatus(run.runId).then(setRun).catch(()=>{}),2000);return()=>clearInterval(timer)},[run?.runId]);
 useEffect(()=>{if(!run?.runId)return;api.evidence(run.runId).then(setEvidence).catch(fail);if(run.state==="HUMAN_TEST")api.gates(run.runId).then(x=>setGate(x[0]||null)).catch(fail)},[run?.runId,run?.phase,run?.state]);
 const selectScreen=(id:string)=>{const next=screens.find(x=>x.id===id)!;setScreen(id);setEvent(next.events[0].id);setApproved(false);setLoop("DESIGN")};
 const approve=async()=>{setError("");try{const result=await api.approve(screenId,eventId);setApproved(true);report(`설계 v${result.designVersion} 확정 · 구현대기열 생성`)}catch(e){fail(e)}};
 const reopen=async()=>{setError("");try{await api.reopen(screenId,eventId);setApproved(false);setLoop("DESIGN");report("설계 재개방 · 이전 구현 결과 STALE")}catch(e){fail(e)}};
 const start=async()=>{setError("");try{const result=await api.run(loop,screenId,eventId);setRun(result);report(`${result.phase} 실행 시작`)}catch(e){fail(e)}};
 const advance=async()=>{if(!run)return;setError("");try{const result=await api.advanceRun(run.runId,true,"UI에서 단계 증거 확인");setRun(result);report(`${result.phase||result.state} 검증 완료`)}catch(e){fail(e)}};
 const cancel=async()=>{if(!run)return;try{const result=await api.cancelRun(run.runId);setRun({...run,...result});report("실행 취소 완료")}catch(e){fail(e)}};
 const retry=async()=>{if(!run)return;try{const result=await api.retryRun(run.runId);setRun(result);setEvidence([]);report("새 실행으로 재시도 시작")}catch(e){fail(e)}};
 const decide=async(decision:"APPROVE"|"REJECT")=>{if(!gate||!run)return;try{const result=await api.decideGate(gate.id,decision);setRun({...run,state:result.state});setGate({...gate,decision});report(`HUMAN_TEST ${decision}`)}catch(e){fail(e)}};
 const openHarness=async(type:"design"|"code")=>{setError("");try{const[list,saved]=await Promise.all([api.harness(type),api.harnessDrafts(type)]);const byId=Object.fromEntries(saved.map(x=>[x.agentId,x]));setAgents(list);setDrafts(Object.fromEntries(list.map(x=>[x.id,byId[x.id]?.content||x.content])));setDraftVersions(Object.fromEntries(list.map(x=>[x.id,byId[x.id]?.version||0])));setAgent(0);setHarnessType(type)}catch(e){fail(e)}};
 const saveDraft=async()=>{if(!harnessType||!agents[agent])return;const id=agents[agent].id;try{const saved=await api.saveHarnessDraft(harnessType,id,drafts[id]||"",draftVersions[id]||0);setDraftVersions(v=>({...v,[id]:saved.version}));report(`${id} draft v${saved.version} 저장`)}catch(e){fail(e)}};
 const publish=async()=>{if(!harnessType)return;try{for(const item of agents){const checked=await api.validateHarness(harnessType,item.id,drafts[item.id]||"");if(!checked.valid)throw new Error(`${item.name}: ${checked.errors.join(", ")}`)}const result=await api.publishHarness(harnessType,drafts);report(`하네스 v${result.version} 원자적 게시`);setHarnessType(null)}catch(e){fail(e)}};
 return <div className="app">
  <header><strong>Forge<b>Flow</b></strong><label>프로젝트 <select aria-label="프로젝트"><option>ForgeFlow Workspace</option>{projects.map(x=><option key={x.id}>{x.name}</option>)}</select></label><nav><button className="active" onClick={()=>report("코드·화면 작업면")}>코드·화면</button><button onClick={()=>openHarness("design")}>설계 세팅</button></nav><button onClick={()=>openHarness(loop==="DESIGN"?"design":"code")}>하네스</button><button onClick={()=>report(`서버 저장됨 · 증거 ${evidence.length}건`)}>저장 상태</button></header>
  <div className="shell">
   <aside className="left"><h3>화면·이벤트 디렉터리</h3>{screens.map(s=><section key={s.id}><button className={s.id===screenId?"selected":""} onClick={()=>selectScreen(s.id)}>{s.id}<small>{s.name}</small></button>{s.id===screenId&&s.events.map(e=><button className={e.id===eventId?"selected event":"event"} onClick={()=>setEvent(e.id)} key={e.id}>{e.id} · {e.name}</button>)}</section>)}</aside>
   <main>
    <div className="context"><span>화면 잠금</span><b>{screenId}</b><span>이벤트 추적 잠금</span><b>{eventId}</b></div>
    <div className="layers"><span>아키텍처</span>{["Controller","Service/Application","Domain·Policy","Repository·Mapper","SQL"].map(x=><button key={x} onClick={()=>report(`${x} 매핑 선택`)}>{x}</button>)}</div>
    <div className="workspace"><div className="hero"><div><small>{event.id} · {event.kind}</small><h1>{screen.name}</h1><p>{event.name}</p></div><em>{approved?"A 설계 확정":"A 설계 필요"}</em></div>
     <div className="split"><article><h2>이벤트 작업면</h2><div className="browser"><div className="dots">● ● ●</div><div className="mock"><h2>{event.name}</h2><div className="filters"><label>검색어<input aria-label="검색어"/></label><label>상태<select aria-label="상태"><option>전체</option><option>진행중</option></select></label><button onClick={()=>report("검색 조건 초기화")}>초기화</button><button className="primary" onClick={()=>report(`${eventId} 조회 실행`)}>조회</button></div><table><thead><tr><th>이벤트</th><th>루프 상태</th><th>현재 단계</th></tr></thead><tbody><tr><td>{eventId}</td><td>{run?.state||"대기"}</td><td>{run?.phase||"-"}</td></tr></tbody></table></div></div></article>
      <article><h2>설계 산출물·구현대기열</h2>{["manifest.json","requirements.ref.json","api-contract.ref.json","data-model.ref.json"].map(x=><div className="artifact" key={x}>{x}<span>{approved?"READY":"LOCKED"}</span></div>)}</article></div>
    </div>
    <div className="threads"><div className="tabs"><button className={loop==="DESIGN"?"active":""} onClick={()=>setLoop("DESIGN")}>설계루프</button><button disabled={!approved} className={loop==="IMPLEMENT"?"active":""} onClick={()=>setLoop("IMPLEMENT")}>구현루프</button><button onClick={()=>openHarness(loop==="DESIGN"?"design":"code")}>📄</button></div><div className="console"><div><b>{loop==="DESIGN"?"A 설계루프":"B 구현루프"}</b><p>{screenId} · {eventId} · 증거 {evidence.length}</p><button onClick={start}>실행</button>{run&&<button onClick={advance} disabled={["CANCELLED","ACCEPTED","HUMAN_TEST"].includes(run.state)}>단계 검증</button>}{run&&!["CANCELLED","ACCEPTED"].includes(run.state)&&<button onClick={cancel}>취소</button>}{run&&["CANCELLED","B_REPAIR"].includes(run.state)&&<button onClick={retry}>재시도</button>}{run?.state==="HUMAN_TEST"&&<><button className="primary" onClick={()=>decide("APPROVE")}>사람 승인</button><button onClick={()=>decide("REJECT")}>반려</button></>}{loop==="DESIGN"&&(approved?<button onClick={reopen}>설계 다시 열기</button>:<button onClick={approve}>설계 확정</button>)}</div><pre>{error&&`BLOCKED: ${error}\n`}{logs.join("\n")||"실행 대기"}</pre></div></div>
   </main>
   <aside className="right"><h3>서버 이벤트 매핑</h3>{Object.entries(trace).map(([key,values])=><section key={key}><b>{key}</b>{values.length?values.map(x=><div key={x}>{x}</div>):<div>연결 없음</div>}</section>)}</aside>
  </div>
  {harnessType&&<div className="overlay"><div className="modal"><div className="modalHead"><h2>{harnessType==="design"?"설계":"코드"} 하네스 · {agents.length}</h2><button onClick={()=>setHarnessType(null)}>닫기</button></div><div className="harness"><aside>{agents.map((x,i)=><button className={i===agent?"selected":""} onClick={()=>setAgent(i)} key={x.id}>{i+1}. {x.name}</button>)}</aside><section>{agents[agent]&&<><code>{agents[agent].file} · draft v{draftVersions[agents[agent].id]||0}</code><textarea value={drafts[agents[agent].id]||""} onChange={e=>setDrafts(v=>({...v,[agents[agent].id]:e.target.value}))}/></>}</section></div><footer><button onClick={saveDraft}>Draft 저장</button><button className="primary" onClick={publish}>검증 후 원자적 게시</button></footer></div></div>}
 </div>
}
