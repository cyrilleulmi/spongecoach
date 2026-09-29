# Glossary: German ↔ English

Ubiquitous language for the SpongeCoach floorball domain. The canonical term is always English; German is the term coaches and players actually use day to day. Code (classes, DB columns, API/JSON fields) uses the English term exclusively — German appears only in UI copy and here.

Referenced from `CONTEXT.md` once that file exists.

## Language

**Line** (German: *Block*):
A group of players who train and play together; a Line's roster is a selection from the Team's Players, and the same Player can be on several Lines (ADR-0008).
_Avoid_: Block (in code/API), squad

**Player** (German: *Spieler*):
A member of the Team, associated with zero or more Lines. Created by seed only in v1.

**Team** (German: *Team* / *Mannschaft*):
The single club team this MVP serves. No multi-team support.

**Roster** (German: *Kader*):
The set of Players associated with a Line (`line_player` n:n — a selection, not ownership).
_Avoid_: squad, lineup

**Skill** (German: *Skill*):
A configurable attribute, seeded with defaults, that a Line self-rates. Shared across Lines — a Line links to an existing Skill rather than owning its own copy.

**Rating** (German: *Bewertung*):
A Line's 0-100 self-assessment of a Skill it's linked to, held as a single current value with no history (not tied to an individual Player).
_Avoid_: score, evaluation

**Player skill** / **Player rating** / **Player development goal** (German: *Skill* / *Bewertung* / *Ziel*):
A Player's own Skills, Ratings and Development goals — separate lists from the Line ones (ADR-0015). The UI uses the same German words as for Lines.

**Development goal** (German: *Ziel*):
A development target. Shared across Lines — a Line links to existing Development goals.
_Avoid_: Goal (reserved — would collide with a scored goal/Tor if that concept is ever modeled; out of scope for this MVP)

**Focus** (German: *Fokus*):
Free text a coach sets per Line per Training/Match, naming what that Line works on. Not a shared catalog — it's typed at the Event, not reused by reference.

**Iteration** (no German pairing — internal/technical concept, not floorball vocabulary):
A named group of Events, ordered relative to other Iterations.

**Training** (German: *Training*):
A practice Event.

**Match** (German: *Match*):
A game Event. Note: German-Swiss usage — not "Spiel".

**Event** (German: *Ereignis*):
Umbrella term covering both Training and Match. Belongs to exactly one Iteration and holds a position within it.
_Avoid_: appointment, item

**Drill** (German: *Übung*):
An exercise drawn on a tactic board and animated by the app (ADR-0018).
_Avoid_: Übung (in code/API)

**Drill sketch** (German: *Skizze* / *Foto*):
One photo of the tactic board belonging to a Drill.

**Drill tag** (German: *Tag*):
A label from a fixed list on a Drill, e.g. *Mit Gegenspielern*, *Wiederholung spiegelverkehrt*.

**Stage** (German: *Stufe*):
One continuous animated run of a Drill; a progression of photos gives several Stages.

**Actor** (German: *Spieler* in UI copy):
A figure in the animation. Not a Player: the UI may say *Spieler*, code never says Player for it.

**Part** (no German pairing — internal concept, not shown in the UI):
What an Actor does in one run; handed to another Actor in a seamless loop.

**Step** (German: *Schritt*): run (*Laufweg*), dribble (*Dribbling*), pass (*Pass*), shot (*Schuss*), roam (*Bewegung*), wait (*Warten*).

**Repetition** (German: *Wiederholung*): replay (*neu starten*), seamless (*nahtlos*), mirrored (*spiegelverkehrt*).

**Clarifying question** (German: *Rückfrage*):
A question the interpreter asks the coach instead of guessing.

**Sequence** (no German pairing — internal/technical concept, not floorball vocabulary):
Ordering by position: each Iteration is positioned relative to other Iterations, each Event relative to the other Events in its Iteration.
