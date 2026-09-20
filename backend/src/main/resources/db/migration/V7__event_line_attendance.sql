-- Event historicization (see ADR-0012): which Lines attend an Event is now a
-- snapshot taken at Event creation, not derived live from the current Line list.
-- This makes a done Event show the Lines that actually attended: a Line added
-- afterwards never joins retroactively, a since-deleted Line still shows on
-- events it attended, and a Line deleted then restored correctly stays off any
-- event created while it was gone.

alter table line add column color varchar(7);

update line set color = '#4c8c3d' where id = '10000000-0000-0000-0000-000000000001'; -- Kiwi
update line set color = '#8b5e34' where id = '10000000-0000-0000-0000-000000000002'; -- Bäri
update line set color = '#6a4c93' where id = '10000000-0000-0000-0000-000000000003'; -- Lama
update line set color = '#c9702c' where id = '10000000-0000-0000-0000-000000000004'; -- Goalies
update line set color = '#3b6ea5' where color is null;

alter table line alter column color set not null;

create table event_line (
    event_id uuid not null references event (id),
    line_id uuid not null references line (id),
    primary key (event_id, line_id)
);

-- Backfill: every Event that already exists is attended by every currently
-- active Line — the best information available for events created before
-- this feature shipped.
insert into event_line (event_id, line_id)
select e.id, l.id
from event e
cross join line l
where l.deleted_at is null;
