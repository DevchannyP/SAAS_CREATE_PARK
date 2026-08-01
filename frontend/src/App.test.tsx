import{render,screen,waitFor}from"@testing-library/react";
import userEvent from"@testing-library/user-event";
import{beforeEach,describe,expect,it,vi}from"vitest";
import{App}from"./App";

const reply=(data:unknown,status=200)=>Promise.resolve(new Response(JSON.stringify(data),{status,headers:{"Content-Type":"application/json"}}));
const trace={requirements:["REQ-01"],tables:["AGC_DOMAIN"],apis:["API-01"],files:["src/events/EVT-01.tsx"],methods:["handleEVT01"]};
const agents=[{id:"requirements",name:"요구사항 명세 Agent",file:"/harness/design/requirements-agent.md",content:"# requirements evidence"}];

describe("ForgeFlow integrated workspace",()=>{
 beforeEach(()=>{
  Object.defineProperty(globalThis.crypto,"randomUUID",{value:()=>"12345678-1234-4234-8234-123456789012",configurable:true});
  vi.stubGlobal("fetch",vi.fn((input:RequestInfo|URL,init?:RequestInit)=>{
   const url=String(input);
   if(url.endsWith("/projects"))return reply([]);
   if(url.includes("/trace"))return reply(trace);
   if(url.endsWith("/design-snapshots/approve"))return reply({state:"IMPLEMENTATION_READY",designVersion:"1"});
   if(url.endsWith("/runs")&&init?.method==="POST")return reply({runId:"run-1",state:"A_REVIEW",phase:"D00_SNAPSHOT_FREEZE"});
   if(url.endsWith("/harnesses/design"))return reply(agents);
   if(url.endsWith("/harnesses/design/drafts"))return reply([]);
   if(url.includes("/evidence?"))return reply([]);
   return reply({});
  }));
 });
 it("loads server trace, changes screen, approves design and starts a run",async()=>{
  const user=userEvent.setup();render(<App/>);
  expect(await screen.findByText("REQ-01")).toBeInTheDocument();
  await user.click(screen.getByRole("button",{name:/SCR-COMMON-CODE/}));
  expect(screen.getByRole("button",{name:/EVT-41/})).toBeInTheDocument();
  await user.click(screen.getByRole("button",{name:"설계 확정"}));
  expect(await screen.findByText("A 설계 확정")).toBeInTheDocument();
  await user.click(screen.getByRole("button",{name:"실행"}));
  expect(await screen.findByText("D00_SNAPSHOT_FREEZE")).toBeInTheDocument();
  expect(fetch).toHaveBeenCalledWith("/api/v1/runs",expect.objectContaining({method:"POST"}));
 });
 it("opens a server-backed harness modal with accessible controls",async()=>{
  const user=userEvent.setup();render(<App/>);await screen.findByText("REQ-01");
  await user.click(screen.getByRole("button",{name:"하네스"}));
  expect(await screen.findByRole("heading",{name:"설계 하네스 · 1"})).toBeInTheDocument();
  expect(screen.getByRole("button",{name:/요구사항 명세 Agent/})).toBeEnabled();
  expect(screen.getByRole("button",{name:"Draft 저장"})).toBeEnabled();
  await user.click(screen.getByRole("button",{name:"닫기"}));
  await waitFor(()=>expect(screen.queryByRole("heading",{name:"설계 하네스 · 1"})).not.toBeInTheDocument());
 });
 it("renders server failures as a visible blocked state",async()=>{
  const mocked=vi.mocked(fetch);mocked.mockImplementation((input:RequestInfo|URL,init?:RequestInit)=>{
   const url=String(input);if(url.endsWith("/projects"))return reply([]);if(url.includes("/trace"))return reply(trace);
   if(url.endsWith("/runs")&&init?.method==="POST")return reply({detail:"Run rejected by policy"},409);return reply({});
  });
  const user=userEvent.setup();render(<App/>);await screen.findByText("REQ-01");
  await user.click(screen.getByRole("button",{name:"실행"}));
  expect(await screen.findByText(/BLOCKED: Run rejected by policy/)).toBeInTheDocument();
 });
});
