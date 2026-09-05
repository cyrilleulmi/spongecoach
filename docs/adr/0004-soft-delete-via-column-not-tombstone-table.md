# Soft-delete via a nullable column, not a tombstone table

Skill, Rating, Development goal, and Focus all need their history preserved rather than hard-deleted. We chose a plain nullable `deleted_at` column on each entity over a separate tombstone/audit table. A tombstone table would better serve compliance/audit requirements (who deleted what, when, from where), but there's no such requirement for a single hardcoded team's personal-use MVP — a column is simpler to query (just filter `deleted_at IS NULL`) and to migrate later if real audit needs emerge.
