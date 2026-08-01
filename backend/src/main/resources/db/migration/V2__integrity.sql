with ranked as (
  select id,row_number() over(partition by screen_id,event_id order by created_at desc,id desc) as position
  from implementation_queue where status='IMPLEMENTATION_READY'
)
update implementation_queue q set status='STALE' from ranked r where q.id=r.id and r.position>1;

create unique index implementation_queue_one_ready
  on implementation_queue(screen_id,event_id) where status='IMPLEMENTATION_READY';
create unique index human_gate_one_per_run_type on human_gate(run_id,gate_type);
create index workloop_run_context_idx on workloop_run(screen_id,event_id,created_at desc);
create index evidence_run_idx on evidence(run_id,created_at);
create index run_phase_run_idx on run_phase(run_id,started_at);

alter table workloop_run add constraint workloop_loop_type_check check(loop_type in ('DESIGN','IMPLEMENT'));
alter table human_gate add constraint human_gate_decision_check check(decision is null or decision in ('APPROVE','REJECT'));
