# SpongeCoach

Domain glossary for the SpongeCoach floorball team collaboration MVP: a single hardcoded Team whose Lines track rosters, self-rated Skills, Development goals, Focuses, and a sequenced timeline of Events.

German pairings and translation notes live in [docs/glossary.md](docs/glossary.md) — use the English term here in all code, APIs, and docs; German appears only in UI copy.

## Language

**Team**:
The single club this MVP serves. No multi-team support.

**Line**:
A group of Players who train and play together. A Player belongs to exactly one Line.
_Avoid_: Block (German term, code/API stays English), squad

**Player**:
A member of a Line's roster.

**Roster**:
The set of Players belonging to a Line.
_Avoid_: squad, lineup

**Skill**:
A configurable attribute a Line self-rates, seeded with defaults but owned independently per Line — editing one Line's Skill list never affects another Line's.

**Rating**:
A Line's 0-5 self-assessment of one of its Skills, recorded as a new entry each time rather than overwritten — so a Skill's rating history is a trend, not just a current value. Not tied to an individual Player.
_Avoid_: score, evaluation, SkillEvaluation

**Development goal**:
A per-Line development target. One or more Development goals give rise to a Focus.
_Avoid_: Goal (reserved — would collide with a scored goal if that concept is ever modeled; out of scope for this MVP)

**Focus**:
A reusable, per-Line item derived from one or more Development goals, attached to one or more Events. Belongs to exactly one Line. Preserved via soft-delete only — never hard-deleted. An Event/Focus attachment itself carries no such history: detaching one is a plain delete.

**Event**:
Umbrella term covering both Training and Match — a single kind of thing, distinguished by its Event type rather than by being separate concepts. Forms a sequenced timeline via a mandatory predecessor/successor chain, independent of its (optional) date.
_Avoid_: appointment, item

**Event type**:
What distinguishes one Event from another (Training, Match). Seeded with those two, but — like Skill — an open, extensible list rather than a fixed pair.

**Training**:
An Event type: a practice.

**Match**:
An Event type: a game. Note: German-Swiss usage — not "Spiel".

**Sequence**:
The mandatory predecessor/successor chain ordering Events, independent of their (optional) dates. Never branches or merges — each Event has at most one predecessor and one successor.
