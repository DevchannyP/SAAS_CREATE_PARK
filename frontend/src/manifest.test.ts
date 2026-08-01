import {describe,expect,it} from "vitest";
import {screens} from "./manifest";

describe("immutable event contract",()=>{
 it("contains exactly five screens and fifteen unique events",()=>{
  expect(screens).toHaveLength(5);
  const events=screens.flatMap(s=>s.events);
  expect(events).toHaveLength(15);
  expect(new Set(events.map(e=>e.id)).size).toBe(15);
 });
 it("keeps event ownership fixed",()=>{
  const owner=new Map(screens.flatMap(s=>s.events.map(e=>[e.id,s.id])));
  expect(owner.get("EVT-01")).toBe("SCR-CONSULT-LIST");
  expect(owner.get("EVT-43")).toBe("SCR-COMMON-CODE");
 });
});
