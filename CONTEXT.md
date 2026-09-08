# SpongeCoach

Domain glossary for the SpongeCoach floorball team collaboration MVP: a single hardcoded Team whose Lines track rosters, self-rated Skills, Development goals, Focuses, and a sequenced timeline of Events.

German pairings and translation notes live in [docs/glossary.md](docs/glossary.md) — use the English term here in all code, APIs, and docs; German appears only in UI copy.

## Language

**Team**:
The single club this MVP serves. No multi-team support.

**Line**:
A group of Players who train and play together. A Line's roster is a selection of the Team's Players — the same Player may be on more than one Line (ADR-0008).
_Avoid_: Block (German term, code/API stays English), squad

**Player**:
A member of the Team, associated with zero or more Lines. Team-scoped like the Skill/Development goal/Focus catalogs; created by seed only in v1 (no create-Player endpoint or UI).

**Roster**:
The set of Players associated with a Line — a Line↔Player selection, not ownership (`line_player` n:n).
_Avoid_: squad, lineup

**Skill**:
A configurable attribute a Line self-rates, seeded with defaults. Shared catalog, not owned per-Line: a Line associates with an existing Skill rather than holding its own copy, so editing the catalog row is visible to every Line linked to it. Coaches can create new Skills from the line UI (ADR-0009); each carries a color chosen from a fixed palette.

**Rating**:
A Line's 0-100 self-assessment of a Skill it's associated with, held as a single current value on that Line-Skill association and overwritten on each update. No history is kept. Not tied to an individual Player.
_Avoid_: score, evaluation, SkillEvaluation

**Development goal**:
A development target. Shared catalog, not owned per-Line: a Line associates with existing Development goals, and can create new ones from the line UI (ADR-0009), each with a palette color. One or more Development goals give rise to a Focus.
_Avoid_: Goal (reserved — would collide with a scored goal if that concept is ever modeled; out of scope for this MVP)

**Focus**:
A reusable item derived from one or more Development goals, attached to one or more Events (per Line, via that Event's Line-Focus-Event link — see Event). Shared catalog, not owned per-Line: a Line associates with existing Focuses, and can create new ones from the line UI (ADR-0009) by naming them and picking one or more originating Development goals. Preserved via soft-delete only — never hard-deleted; deleting a Focus is blocked while any Line is still associated with it. An Event/Focus attachment itself carries no such history: detaching one is a plain delete.

**Iteration**:
A named group of Events, ordered relative to other Iterations. The unit a coach plans around (e.g. a training block or phase).

**Event**:
Umbrella term covering both Training and Match — a single kind of thing, distinguished by its Event type rather than by being separate concepts. Belongs to exactly one Iteration and holds a position within it (see Sequence). A Line attaches a Focus to an Event via a three-way Line-Focus-Event link, since a Focus is no longer implicitly one Line's.
_Avoid_: appointment, item

**Event type**:
What distinguishes one Event from another (Training, Match). Seeded with those two, but — like Skill — an open, extensible list rather than a fixed pair.

**Training**:
An Event type: a practice.

**Match**:
An Event type: a game. Note: German-Swiss usage — not "Spiel".

**Sequence**:
Ordering by position: each Iteration has a position relative to other Iterations, and each Event has a position relative to the other Events in its Iteration. Positions are plain, coach-assigned numbers — nothing enforces they're gapless or unique.
_Avoid_: chain (superseded predecessor/successor model)

## Sample data

The canonical example data for mockups, prototypes, and seed data (see [Seed data content](https://github.com/cyrilleulmi/spongecoach/issues/10)). Use this — not placeholder names — whenever a Line/Player/Skill/Development goal/Focus example is needed.

**The content is written in German** — the language the team actually uses — even though domain *terms* (entity names, API/JSON fields, DB columns) stay English per [docs/glossary.md](docs/glossary.md). Copy these rows verbatim; don't translate them back to English or substitute English placeholders.

**Team players**: Carmela, Debi, Gina, Nives, Rahel, Sophie, Jana, Mara, Anita, Samira, Sabrina, Cheyenne, Stocki

**Lines & Rosters** (the user's real team — each Player below is on the one Line shown, though the model allows a Player on several Lines):

| Line | Players |
|---|---|
| Kiwi | Carmela, Debi, Gina, Nives |
| Bäri | Rahel, Sophie, Jana, Mara |
| Lama | Anita, Samira, Sabrina, Cheyenne |
| Goalies | Stocki |

**Skill catalog**: Passgenauigkeit, Schusshärte, Stellungsspiel, Umschalttempo, Stockführung, Bully-Kontrolle

**Development goal catalog**: Ballverluste im eigenen Drittel reduzieren, Überzahlspiel verbessern, Kompakte Defensive aufbauen, Abschlüsse aus dem Slot erhöhen

**Focus catalog** (each derived from one Development goal):

| Focus | Development goal |
|---|---|
| Spielaufbau aus der tiefen Zone | Kompakte Defensive aufbauen |
| Abschlussübungen 2-auf-1 | Abschlüsse aus dem Slot erhöhen |
| Einläufe im Überzahlspiel | Überzahlspiel verbessern |
| Cross-Pässe unter Druck | Ballverluste im eigenen Drittel reduzieren |
| Rebound-Kontrolle nach Pad-Abwehr | Abschlüsse aus dem Slot erhöhen |
