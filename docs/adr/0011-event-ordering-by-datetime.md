---
status: accepted
---

# Event ordering by scheduled datetime, not position

Supersedes ADR-0007 for Event (Iteration keeps its own plain-integer position — that part of
ADR-0007 stands). `scheduledOn` moves from an optional `date` to a mandatory `timestamp`
(`LocalDateTime`, no timezone — the app is single-team, single-locale) and becomes the sole
ordering key for Events within an Iteration; the `position` column is dropped from `event`
entirely. "Training N" numbering and the timeline's display order are both derived from sorted
`scheduledOn`, never stored separately.

We accepted this because the weaker guarantee ADR-0007 chose for position — no uniqueness, no
gap-filling — never sat well with a field that also means something to the coach: two Events
sharing a position was silently allowed but made no sense once a real schedule exists, since a
coach reading the plan expects one thing to happen at a time. A DB unique constraint on
`(iteration_id, scheduled_on)` now enforces that directly, surfaced as a 409 from the API rather
than left to the client to avoid. This also removes a redundant field: with dates mandatory and
collision-free, `scheduledOn` alone totally orders an Iteration's Events, so keeping `position`
alongside it would just be two sources of truth that could drift.
