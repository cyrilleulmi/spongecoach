---
status: accepted
---

# Skills, Development goals and Focuses are creatable from the line UI; colors come from a fixed palette

"Implement per-line overview page" deliberately kept catalog *creation* out of v1 — the Manage panels only associated a Line to existing rows, and the only write to a catalog was a narrow color-only `PUT`. We've reversed that: a coach can now create a new Skill, Development goal, or Focus directly from the per-line overview's Manage panels.

- `POST /api/skills` and `POST /api/development-goals` take `{name, color}`; `POST /api/focuses` takes `{name, goalIds}` — a `goalIds` array so a Focus can be connected to several Development goals at once (at least one is required, per the glossary). All return `201` with the created row.
- A freshly created catalog row is **auto-associated with the line it was created from** — the coach is in that line's context and expects it to apply there. It stays a shared catalog row, available to associate elsewhere.
- Player creation stays out (ADR-0008) — players are seed-only. Only the three shared catalogs are creatable.

**Colors are chosen from a fixed 8-color palette** (`frontend/src/app/theme/palette.ts`), never free-form. Both the create forms and the "change this row's color" affordance use the same swatch picker. Rationale: keeps the small set of category colors visually distinct and on-brand, and avoids a color-input widget. The palette lives only in the frontend — the backend still stores whatever hex string it's given (`varchar(7)`), so widening or theming the palette later is a frontend-only change.

This is additive and low-risk to reverse (drop the `POST` routes and the create forms); accepted because entering a season's real skills/goals/focuses through seed edits or raw SQL was the main friction left in the per-line workflow.
