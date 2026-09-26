-- Users and their Role (ADR-0017). No authentication yet: the frontend picks a User from a
-- dropdown and the backend trusts the `spongecoach-user` cookie. A PLAYER user acts as exactly one
-- Player; a COACH may also be a Player (Anita); a SYS_ADMIN belongs to no Team.
-- Fixed UUIDs by kind: 9=app_user.

create table app_user (
    id uuid primary key,
    name varchar(100) not null,
    role varchar(20) not null check (role in ('SYS_ADMIN', 'COACH', 'PLAYER')),
    team_id uuid references team (id),
    player_id uuid unique references player (id),
    check (role = 'SYS_ADMIN' or team_id is not null),
    check (role <> 'PLAYER' or player_id is not null)
);

insert into app_user (id, name, role, team_id, player_id) values
    ('90000000-0000-0000-0000-000000000001', 'Admin', 'SYS_ADMIN', null, null),
    ('90000000-0000-0000-0000-000000000002', 'Cyrille', 'COACH', '00000000-0000-0000-0000-000000000001', null),
    ('90000000-0000-0000-0000-000000000003', 'Jan', 'COACH', '00000000-0000-0000-0000-000000000001', null),
    ('90000000-0000-0000-0000-000000000004', 'Samuel', 'COACH', '00000000-0000-0000-0000-000000000001', null),
    ('90000000-0000-0000-0000-000000000005', 'Anita', 'COACH', '00000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000009');

-- Every other seeded Player gets a PLAYER user of the same name.
insert into app_user (id, name, role, team_id, player_id)
select gen_random_uuid(), p.name, 'PLAYER', p.team_id, p.id
from player p
where p.id not in (select player_id from app_user where player_id is not null);
