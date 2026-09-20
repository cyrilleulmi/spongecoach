-- Player attendance for Events.
-- event_line_player snapshots which Line(s) a Player belonged to when this Event's attendance was
-- frozen (Event creation, or a later roster change while the Event was still upcoming) -- mirrors
-- event_line/line_player's snapshot semantics (ADR-0012), just one level deeper: a Player added to
-- a Line afterwards is not retroactively added to a past Event, and one removed still keeps their
-- historic rows on Events they were snapshotted onto.
-- event_attendance holds the Player's single answer for the Event, regardless of how many
-- attending Lines they're snapshotted onto for it (a Player on two attending Lines gets two
-- event_line_player rows but only one event_attendance row).

create table event_line_player (
    event_id uuid not null references event (id),
    line_id uuid not null references line (id),
    player_id uuid not null references player (id),
    primary key (event_id, line_id, player_id)
);

create table event_attendance (
    event_id uuid not null references event (id),
    player_id uuid not null references player (id),
    status varchar(20) not null default 'PENDING' check (status in ('PENDING', 'ATTENDING', 'DECLINED')),
    decline_message text,
    primary key (event_id, player_id)
);

-- Backfill: snapshot every currently-active Line's current roster onto every existing Event,
-- defaulting every player to PENDING -- the best information available, same spirit as V7.
insert into event_line_player (event_id, line_id, player_id)
select el.event_id, el.line_id, lp.player_id
from event_line el
join line_player lp on lp.line_id = el.line_id;

insert into event_attendance (event_id, player_id, status)
select distinct event_id, player_id, 'PENDING'
from event_line_player;
