-- [TEST-MR] SmartCodeReview test data - do not merge.
-- A Player's jersey number, unique per team when set.

alter table player add column jersey_number integer;

create unique index player_team_jersey_number_uq on player (team_id, jersey_number) where jersey_number is not null;
