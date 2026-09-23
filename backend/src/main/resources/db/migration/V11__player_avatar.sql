-- A Player's painted Avatar (ADR-0016): one current PNG per Player, overwritten on save, no history.
-- The bytes live in their own table so Player reads never load them; the timestamp on `player`
-- is what list and detail reads hand out as the avatar version.

alter table player add column avatar_updated_at timestamptz;

create table player_avatar (
    player_id uuid primary key references player (id),
    image bytea not null
);
