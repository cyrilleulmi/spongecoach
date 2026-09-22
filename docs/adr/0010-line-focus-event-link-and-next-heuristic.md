---
status: accepted
---

# One Focus per Line per Event; "next" is the first incomplete event

Two decisions made while implementing the Iteration timeline (issue #12).

**Line-Focus-Event cardinality.** The three-way link recording which Focus a Line has set for an
Event (`line_focus_event`) has primary key `(event_id, line_id)` — at most **one** Focus per Line
per Event. This matches the resolved timeline UX (issue #9): each event dial has one quadrant per
Line, lit or pale, and the detail panel shows one Focus per Line. Setting a Line's Focus for an
event is an upsert; clearing it is a plain delete (the table carries no history — Focus history
lives on the catalog row via soft-delete, ADR-0006). The write API replaces an event's whole
attachment set in one call (`PUT /api/events/{id}` with `focusAttachments`), an empty list clearing
all. Allowing several Focuses per Line per Event was considered and rejected as unused weight for
the MVP; it could be reintroduced by widening the key without breaking the single-Focus reads.

**The "next scheduled event" marker.** The prototype shows a persistent green ring on the "next"
event, independent of selection. Events have only an optional date, and there is no "done" flag, so
there is no schedule to drive this. The page instead marks as "next" the **first event, in
Iteration-position then Event-position order, where at least one Line has no Focus set** — i.e. the
first one still needing attention. This makes the marker a real "what to chase" signal with the
data we have. If real scheduling arrives later, this heuristic is the obvious thing it replaces.

> **Superseded (ADR-0012):** `scheduledOn` became mandatory in ADR-0011, and events now become
> read-only once done. "Next" is now simply the first event, in the same iteration/event order,
> whose `scheduledOn` hasn't passed — not a Focus-completeness signal. A done event that was never
> fully planned no longer steals the marker.

> **Superseded in part (ADR-0014):** Focus stopped being a catalog row referenced by id — it's now
> free text carried directly on `line_focus_event`. The cardinality decision above (one Focus per
> Line per Event, a full-set replace on write) is unchanged.
