-- A Drill's photos can be removed and brought back (ADR-0020). A removed photo keeps its row and
-- its position: script versions refer to photos by position, and a sketch URL is cached for good,
-- so a position is never handed out twice.
alter table drill_sketch add column deleted_at timestamptz;
