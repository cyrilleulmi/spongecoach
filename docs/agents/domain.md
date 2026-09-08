# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`CONTEXT.md`** at the repo root.
- **`docs/adr/`** — read ADRs that touch the area you're about to work in.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The producer skill (`/grill-with-docs`) creates them lazily when terms or decisions actually get resolved.

## File structure

Single-context repo:

```
/
├── CONTEXT.md
├── docs/adr/
│   ├── 0001-....md
│   └── 0002-....md
└── src/ (backend/, frontend/, etc.)
```

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in `CONTEXT.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal — either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/grill-with-docs`).

## Sample data is German

`CONTEXT.md`'s **Sample data** section holds the canonical example rows — Lines, Rosters, and the Skill / Development goal / Focus catalogs. Their **content is written in German** (the language the team uses), even though domain *terms* — entity names, API/JSON fields, DB columns — stay English per the glossary. That split is deliberate: `Skill.name = "Passgenauigkeit"`, not `Skill.name = "Passing accuracy"`.

When you need an example row (seed data, a fixture, a mockup, an issue example), copy it verbatim from that section. Don't translate it back to English, and don't invent English placeholder names.

## UI copy is German

Everything the end user reads in the frontend is written in **German** — section headings, buttons, placeholders, hint and empty-state text, error messages, `aria-label`s, and the `<title>`. Use the glossary's German pairing for domain terms in that copy (Line → *Block*, Roster → *Kader*, Development goal → *Ziel*, Focus → *Fokus*, Rating → *Bewertung*, …); `docs/glossary.md` is the source of truth for which German word to use.

This does **not** reach into code: component/class/field names, CSS classes, routes, API paths and JSON keys, and log messages all stay English. Only the rendered strings are German. Test assertions that match on visible text or `aria-label`s must use the German string.

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (event-sourced orders) — but worth reopening because…_
