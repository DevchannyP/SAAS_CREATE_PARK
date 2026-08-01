import{render,screen,waitFor}from"@testing-library/react";
import userEvent from"@testing-library/user-event";
import{beforeEach,describe,expect,it,vi}from"vitest";
import{App}from"./App";

const reply=(data:unknown,status=200)=>Promise.resolve(new Response(JSON.stringify(data),{status,headers:{"Content-Type":"application/json"}}));
const trace={requirements:["REQ-01"],tables:["AGC_DOMAIN"],apis:["API-01"],files:["src/events/EVT-01.tsx"],methods:["handleEVT01"]};
const designAgents=[{id:"product-design",name:"제품·UX 설계 Agent",file:"/harness/design/requirements-agent.md",content:"# product design"},{id:"system-design",name:"시스템 설계 Agent",file:"/harness/design/api-architecture-agent.md",content:"# system design"},{id:"design-review",name:"설계 검증 Agent",file:"/harness/design/review-agent.md",content:"# design review"}];
const codeAgents=[{id:"implementation",name:"통합 구현 Agent",file:"/harness/code/backend-agent.md",content:"# implementation"},{id:"test-evidence",name:"테스트·증거 Agent",file:"/harness/code/test-agent.md",content:"# test evidence"},{id:"code-review",name:"코드 검증 Agent",file:"/harness/code/review-agent.md",content:"# code review"}];

describe("ForgeFlow integrated workspace",()=>{
 beforeEach(()=>{
  Object.defineProperty(globalThis.crypto,"randomUUID",{value:()=>"12345678-1234-4234-8234-123456789012",configurable:true});
  vi.stubGlobal("fetch",vi.fn((input:RequestInfo|URL,init?:RequestInit)=>{
   const url=String(input);
   if(url.endsWith("/projects"))return reply([]);
   if(url.includes("/trace"))return reply(trace);
   if(url.includes("/design-snapshots?"))return reply([]);
   if(url.includes("/implementation-queue?"))return reply([{status:"IMPLEMENTATION_READY"}]);
   if(url.endsWith("/design-snapshots/approve"))return reply({state:"IMPLEMENTATION_READY",designVersion:"1"});
   if(url.endsWith("/runs")&&init?.method==="POST")return reply({runId:"run-1",state:"A_REVIEW",phase:"D00_SNAPSHOT_FREEZE",activeAgents:[{id:"product-design",name:"제품·UX 설계 Agent",file:"/harness/design/requirements-agent.md"}]});
   if(url.endsWith("/runs/run-1"))return reply({runId:"run-1",state:"A_REVIEW",phase:"D00_SNAPSHOT_FREEZE",phaseStatus:"RUNNING",activeAgents:[{id:"product-design",name:"제품·UX 설계 Agent",file:"/harness/design/requirements-agent.md"}]});
   if(url.endsWith("/harnesses/design"))return reply(designAgents);
   if(url.endsWith("/harnesses/design/drafts"))return reply([]);
   if(url.endsWith("/harnesses/code"))return reply(codeAgents);
   if(url.endsWith("/harnesses/code/drafts"))return reply([]);
   if(url.includes("/evidence?"))return reply([]);
   return reply({});
  }));
 });
 it("loads server trace, changes screen, approves design and starts a run",async()=>{
  const user=userEvent.setup();render(<App/>);
  expect((await screen.findAllByText("REQ-01")).length).toBeGreaterThan(0);
  await user.click(screen.getByRole("button",{name:/SCR-COMMON-CODE/}));
  expect(screen.getByRole("button",{name:/EVT-41/})).toBeInTheDocument();
  await user.click(screen.getByRole("button",{name:"설계 확정"}));
  expect(await screen.findByText("A 설계 확정")).toBeInTheDocument();
  await user.click(screen.getByRole("button",{name:"실행"}));
  expect(await screen.findByText("D00_SNAPSHOT_FREEZE")).toBeInTheDocument();
  expect(screen.getByText("제품·UX 설계 Agent")).toBeInTheDocument();
  expect(fetch).toHaveBeenCalledWith("/api/v1/runs",expect.objectContaining({method:"POST"}));
 });
 it("opens a server-backed harness modal with accessible controls",async()=>{
  const user=userEvent.setup();render(<App/>);await screen.findAllByText("REQ-01");
  await user.click(screen.getByRole("button",{name:"하네스"}));
  expect(await screen.findByRole("heading",{name:"전체 하네스 · 설계 3 · 코드 3"})).toBeInTheDocument();
  expect(screen.getByRole("button",{name:/제품·UX 설계 Agent/})).toBeEnabled();
  expect(screen.getByRole("button",{name:/통합 구현 Agent/})).toBeEnabled();
  await user.click(screen.getByRole("button",{name:/통합 구현 Agent/}));
  expect(screen.getAllByText("구현루프").length).toBeGreaterThan(0);
  expect(screen.getByRole("button",{name:"Draft 저장"})).toBeEnabled();
  await user.click(screen.getByRole("button",{name:"닫기"}));
  await waitFor(()=>expect(screen.queryByRole("heading",{name:"전체 하네스 · 설계 3 · 코드 3"})).not.toBeInTheDocument());
 });
 it("shows SSOT design details and sends an approved feature to code workspace",async()=>{
  const user=userEvent.setup();render(<App/>);await screen.findAllByText("REQ-01");
  expect(screen.getByRole("heading",{name:/설계 화면/})).toBeInTheDocument();
  expect(screen.getByRole("heading",{name:"목업"})).toBeInTheDocument();
  expect(screen.getByRole("heading",{name:"SSOT 설계 정보"})).toBeInTheDocument();
  expect(screen.getAllByText("AGC_DOMAIN").length).toBeGreaterThan(0);
  await user.click(screen.getByRole("button",{name:"설계 확정"}));
  await user.click(screen.getByRole("button",{name:"코드 작성"}));
  expect(screen.getByRole("heading",{name:"코드 작성"})).toBeInTheDocument();
  expect(screen.getByText("DESIGN APPROVED")).toBeInTheDocument();
  await user.click(screen.getByRole("button",{name:/ForgeFlowApplication/}));
  expect(screen.getByText(/SpringApplication\.run\(ForgeFlowApplication\.class/)).toBeInTheDocument();
  await user.click(screen.getByRole("button",{name:"Controller"}));
  expect(screen.getByText(/class ArchitectureController/)).toBeInTheDocument();
 });
 it("renders server failures as a visible blocked state",async()=>{
  const mocked=vi.mocked(fetch);mocked.mockImplementation((input:RequestInfo|URL,init?:RequestInit)=>{
   const url=String(input);if(url.endsWith("/projects"))return reply([]);if(url.includes("/trace"))return reply(trace);if(url.includes("/design-snapshots?")||url.includes("/implementation-queue?"))return reply([]);
   if(url.endsWith("/runs")&&init?.method==="POST")return reply({detail:"Run rejected by policy"},409);return reply({});
  });
  const user=userEvent.setup();render(<App/>);await screen.findAllByText("REQ-01");
  await user.click(screen.getByRole("button",{name:"실행"}));
  expect(await screen.findByText(/BLOCKED: Run rejected by policy/)).toBeInTheDocument();
 });
});
