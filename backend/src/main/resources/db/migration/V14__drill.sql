-- Drills animated from tactic-board photos (ADR-0018), interpreted by Claude as background jobs
-- (ADR-0019). A Drill's animation is one jsonb document per version; nothing queries inside it.
-- Fixed UUIDs by kind: a=drill_tag.

create table drill_tag (
    id uuid primary key,
    name varchar(100) not null unique,
    position int not null
);

create table drill (
    id uuid primary key,
    team_id uuid not null references team (id),
    name varchar(200) not null,
    sketch_relation varchar(20) not null check (sketch_relation in ('PROGRESSION', 'CONTINUOUS', 'MIXED')),
    status varchar(20) not null check (status in ('PENDING', 'NEEDS_INPUT', 'READY', 'FAILED')),
    -- Why the last job failed, readable for the coach; null unless FAILED.
    error text,
    -- The drill_script_version shown and edited; null until the interpreter produced one.
    current_version int,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz
);

create table drill_drill_tag (
    drill_id uuid not null references drill (id),
    drill_tag_id uuid not null references drill_tag (id),
    primary key (drill_id, drill_tag_id)
);

-- One photo of the board. `reading` is the interpreter's latest symbol list for it (ADR-0019).
create table drill_sketch (
    id uuid primary key,
    drill_id uuid not null references drill (id),
    position int not null,
    note text,
    image bytea not null,
    reading jsonb,
    unique (drill_id, position)
);

-- Every AI change, manual edit and revert is a new version; undo is a revert (ADR-0018).
create table drill_script_version (
    id uuid primary key,
    drill_id uuid not null references drill (id),
    version int not null,
    source varchar(10) not null check (source in ('AI', 'EDIT', 'REVERT')),
    script jsonb not null,
    change_summary text,
    created_at timestamptz not null,
    unique (drill_id, version)
);

-- The conversation with the interpreter, append-only: the coach's answers and chat messages, and
-- the interpreter's replies (Clarifying questions, or the script version it produced).
create table drill_message (
    id uuid primary key,
    drill_id uuid not null references drill (id),
    position int not null,
    author varchar(12) not null check (author in ('COACH', 'INTERPRETER')),
    content text,
    questions jsonb,
    answers jsonb,
    script_version int,
    created_at timestamptz not null,
    unique (drill_id, position)
);

insert into drill_tag (id, name, position) values
    ('a0000000-0000-0000-0000-000000000001', 'Mit Gegenspielern', 1),
    ('a0000000-0000-0000-0000-000000000002', 'Ohne Gegenspieler', 2),
    ('a0000000-0000-0000-0000-000000000003', 'Mit Goalie', 3),
    ('a0000000-0000-0000-0000-000000000004', 'Überzahl', 4),
    ('a0000000-0000-0000-0000-000000000005', 'Schiessen', 5),
    ('a0000000-0000-0000-0000-000000000006', 'Passen', 6),
    ('a0000000-0000-0000-0000-000000000007', 'Stockführung', 7),
    ('a0000000-0000-0000-0000-000000000008', 'Spielaufbau', 8),
    ('a0000000-0000-0000-0000-000000000009', 'Aufwärmen', 9),
    ('a0000000-0000-0000-0000-00000000000a', 'Wiederholung spiegelverkehrt', 10);
