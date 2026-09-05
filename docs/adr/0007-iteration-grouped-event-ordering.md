---
status: accepted
---

# Iteration-grouped Event ordering, not a linked list

Supersedes ADR-0001. The self-referencing linked-list chain (`previous_event_id`/`next_event_id`) is replaced with a two-level ordering: a new Iteration entity groups Events, and both levels order by a plain integer position — Iterations relative to each other, Events relative to their siblings within an Iteration. Events no longer reference each other at all.

This is a genuine trade-off, not a refinement of the same idea: the linked list gave a strict, gapless, branch-proof chain at the cost of FK-splicing on every insert/delete; integer positions are simpler to read and write (a coach reorders one Event with a single field update) but enforce nothing — no uniqueness, no gap-filling, duplicates and holes are allowed by design. We accepted the weaker guarantee because the chain's strictness was never actually load-bearing for this MVP (nothing consumes "no gaps" as a rule), while the simpler model directly enables the Iteration grouping a coach thinks in (a training block or phase), which the flat chain had no way to represent. Iterations carry the same loose integer-position scheme, anticipating that Iterations may later reference each other more richly (e.g. an iteration-level overview linking into per-iteration detail views) — that richer relationship isn't specified yet.
