import{useEffect,useState}from"react";
import{api,type MagazineGroup,type MagazineJob,type MagazineVideo}from"./api";
import"./youtube-magazine.css";
import"./youtube-magazine-actions.css";

const stages=["수집","그룹화","랭킹","핫파트","대본","스케치","TTS","편집","품질검사","업로드"];

export function YoutubeMagazineApp(){
 const[jobs,setJobs]=useState<MagazineJob[]>([]),[videos,setVideos]=useState<MagazineVideo[]>([]),[groups,setGroups]=useState<MagazineGroup[]>([]),[busy,setBusy]=useState(false),[error,setError]=useState(""),[notice,setNotice]=useState("");
 const load=()=>Promise.all([api.magazineJobs(),api.magazineVideos(),api.magazineGroups()]).then(([j,v,g])=>{setJobs(j);setVideos(v);setGroups(g)}).catch(e=>setError(e.message));
 useEffect(()=>{load()},[]);
 const command=async(action:()=>Promise<unknown>,message?:string)=>{setBusy(true);setError("");setNotice("");try{await action();await load();if(message)setNotice(message)}catch(e){setError(e instanceof Error?e.message:"요청 실패")}finally{setBusy(false)}};
 return <div className="ymApp">
  <header className="ymHeader"><div><a href="/">← ForgeFlow</a><small>ISOLATED CONTENT PIPELINE</small><h1>YouTube Hot 6</h1><p>메타데이터 기반 랭킹 매거진 제작 콘솔</p></div><div className="ymActions"><button className="secondary" disabled={busy} onClick={()=>command(()=>api.collectMagazineVideos(),"수집·랭킹·TOP 6 그룹 생성 완료")}>인기 영상 수집</button><button disabled={busy||groups.length===0} onClick={()=>command(()=>api.createMagazineJob("SHORTS",groups[0]?.id),"최신 TOP 6 제작 작업 생성 완료")}>+ 제작 작업 생성</button></div></header>
  {error&&<div className="ymError">{error}</div>}
  {notice&&<div className="ymNotice">{notice}</div>}
  <section className="ymPolicy"><b>안전 모드</b><span>원본 영상 다운로드 금지</span><span>스케치 신규 생성</span><span>기본 비공개</span><span>승인 후 업로드</span></section>
  <section className="ymPipeline">{stages.map((stage,index)=><div key={stage}><i>{index+1}</i><span>{stage}</span></div>)}</section>
  <main className="ymGrid">
   <section className="ymPanel ymJobs"><div className="ymTitle"><div><small>PIPELINE JOBS</small><h2>제작 작업</h2></div><strong>{jobs.length}</strong></div>
    {jobs.length===0?<div className="ymEmpty">아직 작업이 없습니다.<br/>테스트 작업을 생성해 승인 흐름을 확인하세요.</div>:jobs.map(job=><article key={job.id}><div><b>{job.format}</b><code>{job.id.slice(0,8)}</code><span className={`ymStatus ${job.status}`}>{job.status}</span></div><p>{job.stage} · 공개 설정 {job.privacyStatus}</p><progress value={job.progress} max="100"/><footer><span>{job.progress}%</span>{job.status==="DRAFT"&&<button disabled={busy} onClick={()=>command(()=>api.approveMagazineJob(job.id))}>검수 승인</button>}{job.status==="APPROVED"&&<button disabled={busy} onClick={()=>command(()=>api.prepareMagazineUpload(job.id))}>비공개 업로드 준비</button>}</footer></article>)}
   </section>
   <section className="ymPanel"><div className="ymTitle"><div><small>TRENDING SOURCES</small><h2>수집 영상</h2></div><strong>{videos.length}</strong></div>{videos.length===0?<div className="ymEmpty">API 키 연결 전입니다.<br/>현재는 외부 수집이 비활성화되어 있습니다.</div>:videos.map(v=><article key={v.id}><b>{v.title}</b><p>{v.channelTitle}</p><span>HOT {v.hotScore}</span></article>)}</section>
   <section className="ymPanel"><div className="ymTitle"><div><small>TOP 6 CLUSTERS</small><h2>랭킹 그룹</h2></div><strong>{groups.length}</strong></div>{groups.length===0?<div className="ymEmpty">수집 데이터가 준비되면<br/>6개 묶음이 여기에 표시됩니다.</div>:groups.map(g=><article key={g.id}><b>{g.groupTitle}</b><p>{g.topicKeyword} · {g.itemCount}/6</p></article>)}</section>
  </main>
  <footer className="ymFooter"><b>Scaffold v1</b><span>외부 API·TTS·이미지·FFmpeg·실제 업로드 어댑터는 의도적으로 잠겨 있습니다.</span></footer>
 </div>
}
