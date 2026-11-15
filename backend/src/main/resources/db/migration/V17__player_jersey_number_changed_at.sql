-- [TEST-MR] SmartCodeReview test data - do not merge.
-- When a Player's jersey number was last set, for the team's number history.

alter table player add column jersey_number_changed_at timestamp with time zone;
