---
status: accepted
---

# Skill/Development goal/Focus as shared catalogs; Rating as a scalar, not a history

Skill, Development goal, and Focus were originally modeled as owned independently per-Line (ADR-0003's companion decision, "Core domain model & schema"). We've reversed that: all three are now shared catalogs, with a Line linking to existing rows via an n:n association rather than holding its own copy. This is hard to reverse once real data exists (splitting a shared catalog back into per-Line copies means deciding who "owns" each row's history), and it changes what editing a catalog row means — a coach editing a Skill's label now affects every Line linked to it, not just their own. Because a shared row can quietly vanish out from under a Line, deleting a catalog row is blocked while any Line is still associated with it (for Focus specifically, an Event attachment alone does not block deletion — only a live Line-Focus association does, since Focus history is already preserved via soft-delete).

Alongside this, Rating drops the append-only history model: it's now a single 0-100 scalar (previously 0-5) stored directly on the Line-Skill association and overwritten on each update, with no history table. We picked a wider 0-100 range specifically to give the frontend more room to visualize gradation. This is a real trade-off — the original design used the rating history to render a trend, and that capability is now gone — accepted because a coach's live current assessment matters more for the MVP than trend charting, and it can be reintroduced later as an additive change (an append-only table can be added back without breaking the scalar field) if the trend view turns out to be worth it.
