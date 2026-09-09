-- Event ordering by scheduled datetime, not position (ADR-0011). scheduled_on widens from an
-- optional date to a mandatory, per-Iteration-unique timestamp; position is retired.

alter table event alter column scheduled_on type timestamp using scheduled_on::timestamp;

-- Reschedule the seed's Events into a real 2026 calendar: Trainings alternate Monday/Thursday,
-- each Iteration closing on a Sunday Match. Vorbereitung sits in the past (already played),
-- Meisterschaft – Hinrunde in the future (still to play) — a realistic mixed timeline.
-- Vorbereitung (past)
update event set scheduled_on = timestamp '2026-08-03 18:00' where id = '70000000-0000-0000-0000-000000000001';
update event set scheduled_on = timestamp '2026-08-06 18:00' where id = '70000000-0000-0000-0000-000000000002';
update event set scheduled_on = timestamp '2026-08-10 18:00' where id = '70000000-0000-0000-0000-000000000003';
update event set scheduled_on = timestamp '2026-08-13 18:00' where id = '70000000-0000-0000-0000-000000000004';
update event set scheduled_on = timestamp '2026-08-17 18:00' where id = '70000000-0000-0000-0000-000000000005';
update event set scheduled_on = timestamp '2026-08-20 18:00' where id = '70000000-0000-0000-0000-000000000006';
update event set scheduled_on = timestamp '2026-08-23 15:00' where id = '70000000-0000-0000-0000-000000000007';
-- Meisterschaft – Hinrunde (future)
update event set scheduled_on = timestamp '2026-09-14 18:00' where id = '70000000-0000-0000-0000-000000000011';
update event set scheduled_on = timestamp '2026-09-17 18:00' where id = '70000000-0000-0000-0000-000000000012';
update event set scheduled_on = timestamp '2026-09-21 18:00' where id = '70000000-0000-0000-0000-000000000013';
update event set scheduled_on = timestamp '2026-09-24 18:00' where id = '70000000-0000-0000-0000-000000000014';
update event set scheduled_on = timestamp '2026-09-28 18:00' where id = '70000000-0000-0000-0000-000000000015';
update event set scheduled_on = timestamp '2026-10-01 18:00' where id = '70000000-0000-0000-0000-000000000016';
update event set scheduled_on = timestamp '2026-10-04 15:00' where id = '70000000-0000-0000-0000-000000000017';

alter table event alter column scheduled_on set not null;
alter table event add constraint uq_event_iteration_scheduled_on unique (iteration_id, scheduled_on);
alter table event drop column position;
