export type EventKind="QUERY"|"COMMAND"|"NAVIGATION"|"EXPORT";
export type ScreenEvent={id:string;name:string;kind:EventKind};
export type Screen={id:string;name:string;events:ScreenEvent[]};
export type LoopType="DESIGN"|"IMPLEMENT";
export type RunState="A_DRAFT"|"A_REVIEW"|"A_APPROVED"|"IMPLEMENTATION_READY"|"B_RUNNING"|"B_REPAIR"|"HUMAN_TEST"|"ACCEPTED"|"STALE";
export type TraceSummary={requirements:string[];tables:string[];apis:string[];files:string[];methods:string[]};
