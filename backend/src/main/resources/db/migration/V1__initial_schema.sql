create table team (
    id uuid primary key,
    name varchar(100) not null
);

create table line (
    id uuid primary key,
    team_id uuid not null references team (id),
    name varchar(100) not null
);

create table player (
    id uuid primary key,
    team_id uuid not null references team (id),
    name varchar(100) not null
);

create table line_player (
    line_id uuid not null references line (id),
    player_id uuid not null references player (id),
    primary key (line_id, player_id)
);

create table skill (
    id uuid primary key,
    name varchar(100) not null,
    color varchar(7) not null,
    deleted_at timestamptz
);

create table line_skill (
    line_id uuid not null references line (id),
    skill_id uuid not null references skill (id),
    rating int not null check (rating between 0 and 100),
    primary key (line_id, skill_id)
);

create table development_goal (
    id uuid primary key,
    name varchar(100) not null,
    color varchar(7) not null,
    deleted_at timestamptz
);

create table line_development_goal (
    line_id uuid not null references line (id),
    development_goal_id uuid not null references development_goal (id),
    primary key (line_id, development_goal_id)
);

create table focus (
    id uuid primary key,
    name varchar(100) not null,
    deleted_at timestamptz
);

create table focus_development_goal (
    focus_id uuid not null references focus (id),
    development_goal_id uuid not null references development_goal (id),
    primary key (focus_id, development_goal_id)
);

create table line_focus (
    line_id uuid not null references line (id),
    focus_id uuid not null references focus (id),
    primary key (line_id, focus_id)
);
