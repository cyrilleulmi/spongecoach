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
A configurable attribute, seeded with defaults, that a Line self-rates. Shared across Lines — a Line links to an existing Skill rather than owning its own copy.

**Rating** (German: *Bewertung*):
A Line's 0-100 self-assessment of a Skill it's linked to, held as a single current value with no history (not tied to an individual Player).
_Avoid_: score, evaluation

**Development goal** (German: *Ziel*):
A development target. Shared across Lines — a Line links to existing Development goals.
_Avoid_: Goal (reserved — would collide with a scored goal/Tor if that concept is ever modeled; out of scope for this MVP)

**Focus** (German: *Fokus*):
A reusable item attached to one or more Trainings/Matches (per Line, at each attachment). Shared across Lines — a Line links to existing Focuses. History is preserved via soft-delete only — never hard-deleted.

**Iteration** (no German pairing — internal/technical concept, not floorball vocabulary):
A named group of Events, ordered relative to other Iterations.

**Training** (German: *Training*):
A practice Event.

**Match** (German: *Match*):
A game Event. Note: German-Swiss usage — not "Spiel".

**Event** (German: *Ereignis*):
Umbrella term covering both Training and Match. Belongs to exactly one Iteration and holds a position within it.
_Avoid_: appointment, item

**Sequence** (no German pairing — internal/technical concept, not floorball vocabulary):
Ordering by position: each Iteration is positioned relative to other Iterations, each Event relative to the other Events in its Iteration.
