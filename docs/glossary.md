# Glossary: German ↔ English

Ubiquitous language for the SpongeCoach floorball domain. The canonical term is always English; German is the term coaches and players actually use day to day. Code (classes, DB columns, API/JSON fields) uses the English term exclusively — German appears only in UI copy and here.

Referenced from `CONTEXT.md` once that file exists.

## Language

**Line** (German: *Block*):
A group of players who train and play together; a Player belongs to exactly one Line.
_Avoid_: Block (in code/API), squad

**Player** (German: *Spieler*):
A member of a Line's roster.

**Team** (German: *Team* / *Mannschaft*):
The single club team this MVP serves. No multi-team support.

**Roster** (German: *Kader*):
The set of Players belonging to a Line.
_Avoid_: squad, lineup

**Skill** (German: *Skill*):
A per-Line configurable attribute, seeded with defaults, that a Line self-rates.

**Rating** (German: *Bewertung*):
A Line's 0-5 self-assessment of a Skill, tracked over time (not tied to an individual Player).
_Avoid_: score, evaluation

**Development goal** (German: *Ziel*):
A per-Line development target.
_Avoid_: Goal (reserved — would collide with a scored goal/Tor if that concept is ever modeled; out of scope for this MVP)

**Focus** (German: *Fokus*):
A reusable, per-Line item attached to one or more Trainings/Matches. Belongs to exactly one Line. History is preserved via soft-delete only — never hard-deleted.

**Training** (German: *Training*):
A practice Event.

**Match** (German: *Match*):
A game Event. Note: German-Swiss usage — not "Spiel".

**Event** (German: *Ereignis*):
Umbrella term covering both Training and Match. Forms a sequenced timeline via a mandatory predecessor/successor chain (dates optional).
_Avoid_: appointment, item

**Sequence** (no German pairing — internal/technical concept, not floorball vocabulary):
The mandatory predecessor/successor chain ordering Events, independent of their (optional) dates.
