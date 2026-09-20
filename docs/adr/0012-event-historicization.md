---
status: accepted
---

# Event historicization: done Events are read-only, attendance is a creation-time snapshot

Supersedes the "next" heuristic half of ADR-0010 (its Line-Focus-Event cardinality decision
stands). Once `scheduledOn` became mandatory and collision-free (ADR-0011), an Event finally has a
real notion of "done" — its `scheduledOn` is in the past — and the timeline needed to do three
things with that: stop the coach from editing a done Event by accident, keep the "next" marker
pointed at what's actually upcoming, and show a past Event with the Lines that actually attended
it rather than today's Line list.

**"Done" is computed, not stored.** No `isDone` column or flag exists anywhere. Both the frontend
(read-only gating) and the "next" marker compare `scheduledOn` against the current time on read.
The backend does not block writes to a done Event — `EventResource`/`IterationResource` accept the
same PUT/DELETE regardless of date. The read-only behavior is a frontend default only, to prevent
accidental edits; the coach can lift it for a given event via an explicit "edit anyway" toggle
(`EventDetail`), since the backend was never going to stop a deliberate correction.

**"Next" is now date-based.** `TeamOverview.nextEventId` picks the first event, in
Iteration-then-`scheduledOn` order, whose `scheduledOn` hasn't passed. This replaces ADR-0010's
Focus-completeness heuristic, which could point at a done event that was never fully planned — now
that "done" is a real concept, that event should drop out of "next" regardless of its Focus state.

**Attendance is a snapshot taken at Event creation, not derived from Line lifetime.** The obvious
alternative — compute "Lines present" from `Line.createdAt`/`deletedAt` against the Event's
`scheduledOn` at read time — was considered and rejected: `deletedAt` is a single nullable column
that gets cleared on restore (`LineResource.restore`), so it cannot represent "deleted, then later
restored" as a time interval. An Event created *during* a Line's deleted window would incorrectly
show that Line once it was restored, since by read time `deletedAt` would be `null` again.

Instead, `Event` carries a real `@ManyToMany` to `Line` via a plain join table (`event_line`,
no payload — same shape as `line_player`), populated once when the Event is created
(`IterationResource.persistEvent` sets `event.lines` to every currently active Line) and never
recomputed. This gets all three requirements right for free, with no interval-tracking machinery:
a Line created afterwards is never retroactively added to a past Event's `event_line` rows; a
since-deleted Line still resolves (it's a plain FK, like `LineFocusEvent.line` already does across
a Line's soft-delete); and a Line deleted-then-restored is correctly absent from any Event created
while it was gone, since the snapshot at that Event's creation simply never included it.

This is also the seam for a future "don't invite every Line to this Event" feature (mentioned in
CONTEXT.md's Event glossary entry as a likely follow-up): `event.lines` becomes coach-editable per
Event instead of always being every active Line at creation time. `EventResource` already
validates that a Focus attachment's Line is in `event.lines` before accepting it, so that
constraint doesn't need revisiting when attendance becomes selectable.

**Line dial colors are now persisted, not positional.** `Line.color` is assigned once at creation
(`LineResource.create`, cycling a fixed 8-color palette) instead of being derived from a Line's
index in the current, filtered Line list (as the frontend `dial-palette.ts` used to do). A
positional scheme would reshuffle every other Line's color whenever a Line was added or deleted,
and couldn't give a deleted Line a stable color at all for historic dials. `LineSummaryDto` and
`CatalogRefDto` (reused for `EventDto.lines`) both expose it.

**Migration backfill.** Events that existed before this shipped have no real attendance history,
so `V7__event_line_attendance.sql` backfills `event_line` with every currently-active Line for
every existing Event — the best information available, not a claim about who actually attended.
