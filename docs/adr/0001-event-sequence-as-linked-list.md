---
status: superseded by ADR-0007
---

# Event sequence as a linked list, not a position column

The Event timeline needs a strict, gapless predecessor/successor chain independent of (optional) dates. We considered an integer `sequence_position` column, which is simpler to query in bulk order but requires renumbering (or fractional-position hacks) on insertion. We chose nullable self-referencing `previous_event_id`/`next_event_id` FKs instead, with unique DB constraints on both columns to prevent branching or merging. This makes single-event insertion/removal a constant number of FK updates, at the cost of needing a recursive query (or app-side walk) to materialize the ordered timeline.
