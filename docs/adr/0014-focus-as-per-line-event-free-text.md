---
status: accepted
---

# Focus becomes free text per (Line, Event), not a Development-goal-derived catalog

Focus was a shared catalog (ADR-0006): a named row derived from one or more Development goals,
which a Line associated with directly and which a Line-Focus-Event link then referenced per Event.
In practice that indirection didn't earn its keep — a Focus is always spoken for one specific
Training or Match, and coaches want to type it there rather than maintain a separate reusable list
first. We've dropped the catalog: `line_focus_event` now carries the Focus as plain text
(`focus`), one per (Event, Line), and the `Line ↔ Focus` association, the Focus catalog table, and
its Development-goal derivation (`focus_development_goal`) are all gone. The line screen no longer
shows Focuses at all — planning a Focus happens only from the event/timeline screen, per line, per
training.

Losing the catalog means a Focus can no longer be reused by reference, and its history collapses
to whatever text sits on each event's attachment row — no more tracing "every Event this catalog
Focus was ever attached to." We accept that: v1 never built a view for that trace, and free text is
what coaches actually wanted. To keep the common case ("same focus as last time") cheap without
reintroducing a reference, the frontend offers "same focus again" per Line: it looks at that Line's
most recent earlier Event with a Focus set and copies its text into the field. This is a plain
copy, not a link — editing one Event's Focus text never touches another's.
