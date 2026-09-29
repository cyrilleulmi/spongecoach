# SpongeCoach

Domain glossary for the SpongeCoach floorball team collaboration MVP: a single hardcoded Team whose Lines track rosters, self-rated Skills, Development goals, Focuses, and a sequenced timeline of Events.

German pairings and translation notes live in [docs/glossary.md](docs/glossary.md) — use the English term here in all code, APIs, and docs; German appears only in UI copy.

## Language

**Team**:
The single club this MVP serves. No multi-team support.

**Line**:
A group of Players who train and play together. A Line's roster is a selection of the Team's Players — the same Player may be on more than one Line (ADR-0008).
_Avoid_: Block (German term, code/API stays English), squad

**User**:
Someone using the app, with exactly one Role. Seeded only; picked from a dropdown in the header until there is authentication (ADR-0017). A Player-Role User acts as one Player; a Coach may also be a Player.
_Avoid_: account, login (there is none yet)

**Role**:
What a User may change — every User may read everything. **SysAdmin**: everything. **Coach**: everything in their Team, including done Events. **Player**: their own Player, the Lines they are on, those Lines' Focus per Event, and their own attendance, the last two only before the Event (ADR-0017).

**Player**:
A member of the Team, associated with zero or more Lines. Team-scoped like the Skill/Development goal/Focus catalogs; created by seed only in v1 (no create-Player endpoint or UI). Carries their own Player ratings and Player development goals (ADR-0015); shown with their Avatar, or their initials while they have none.

**Avatar**:
A picture a Player paints of themselves in the app — never a photo upload. One current square PNG per Player, overwritten on each save, no history; shown cropped to a circle. Removing it goes back to initials (ADR-0016). Called "Profilbild" in the UI.
_Avoid_: photo, profile picture (in code), PFP

**Player skill**:
A Skill a Player is rated on. Its own list with a palette color, separate from the Line Skill list — no shared rows, and a Line Skill can't be rated on a Player (ADR-0015). Called "Skill" in the UI. Created from the player screen.

**Player rating**:
A Player's 0-100 Rating on a Player skill, one current value per (Player, Player skill), overwritten on each update, no history. Independent of any Line Rating — neither rolls up into the other.

**Player development goal**:
A Development goal for one Player. Its own list with a palette color, separate from the Line Development goal list (ADR-0015). Called "Ziel" in the UI.

**Roster**:
The set of Players associated with a Line — a Line↔Player selection, not ownership (`line_player` n:n).
_Avoid_: squad, lineup

**Skill**:
A configurable attribute a Line self-rates, seeded with defaults. Shared catalog, not owned per-Line: a Line associates with an existing Skill rather than holding its own copy, so editing the catalog row is visible to every Line linked to it. Coaches can create new Skills from the line UI (ADR-0009); each carries a color chosen from a fixed palette.

**Rating**:
A Line's 0-100 self-assessment of a Skill it's associated with, held as a single current value on that Line-Skill association and overwritten on each update. No history is kept. Not tied to an individual Player — a Player's own assessment is a Player rating.
_Avoid_: score, evaluation, SkillEvaluation

**Development goal**:
A development target. Shared catalog, not owned per-Line: a Line associates with existing Development goals, and can create new ones from the line UI (ADR-0009), each with a palette color.
_Avoid_: Goal (reserved — would collide with a scored goal if that concept is ever modeled; out of scope for this MVP)

**Focus**:
Free text a coach — or a Player on that Line — sets per Line per Event (via that Event's Line-Focus-Event link — see Event), naming what that Line works on for that Training or Match. Not a catalog — there's nothing to associate a Line with outside an Event, and no id to reuse; "same focus again" is a plain copy of a Line's most recent earlier Focus text, not a reference (ADR-0014). Setting an Event's attachments replaces the whole set; leaving a Line out of the set clears its Focus for that Event.

**Iteration**:
A named group of Events, ordered relative to other Iterations. The unit a coach plans around (e.g. a training block or phase).

**Event**:
Umbrella term covering both Training and Match — a single kind of thing, distinguished by its Event type rather than by being separate concepts. Belongs to exactly one Iteration and holds a position within it (see Sequence). A Line attaches a free-text Focus to an Event via a three-way Line-Focus-Event link — at most one Focus per Line per Event (ADR-0010, ADR-0014). An Event has a fixed set of attending Lines, snapshotted once when the Event is created — every Line active at that moment, for now (ADR-0012); a likely future feature is letting a coach edit that set per Event, so not every Line has to be invited. An Event becomes read-only in the frontend once its `scheduledOn` has passed (ADR-0012) — a default the coach can lift per Event, not a backend restriction.
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

**Drill**:
An exercise a coach draws on a tactic board and the app animates on a small-court rink (ADR-0018). Has a name, Drill sketches, Drill tags, a sketch relation and one or more Stages. Team-scoped; not linked to Events or Focus. Created, edited and deleted by a Coach only; soft-deleted.
_Avoid_: exercise (in code), Übung (in code/API)

**Drill sketch**:
One photo of the tactic board belonging to a Drill, with its position and an optional note. Up to 12 per Drill. Several sketches are either a progression (each starts a new Stage) or a continuation (the next phase of the same run).

**Drill tag**:
A label from a fixed, seeded list (e.g. "Mit Gegenspielern", "Wiederholung spiegelverkehrt") a coach puts on a Drill. Filters the Drill list, and is a hint to the interpreter, never a fact — a drawing that contradicts a tag gets a Clarifying question.

**Stage**:
One continuous animated run of a Drill. Progression photos make separate Stages (1 v G → 1 v 1 → 2 v 1); continuation photos extend one. Holds Actors, Parts, Props, Steps and a Repetition.

**Actor**:
A figure in a Stage's animation: player, goalie or coach, on side A, B or neutral. Not a Player — a Drill is about positions, not about who in the Team plays them. Keeps its id across Stages.
_Avoid_: Player (reserved for a Team member)

**Part**:
What an Actor does in one run (the passer, the middle player). Steps refer to Parts, so a seamless loop can hand them to different Actors on the next run.
_Avoid_: role (reserved for a User's Role)

**Step**:
One action in a Stage: run, dribble, pass, shot, roam or wait, with a path, a speed, and an order given as "after another Step plus a delay". A run may carry the ball; a dribble is active stickhandling; a roam is loose, undefined movement inside an area.

**Repetition**:
How a Stage's animation loops: *replay* (reset and play again) or *seamless* (Actors move into their next Parts and carry on), optionally *mirrored* (the next run comes from the other side, reflected across the rink's long axis).

**Drill script**:
A Drill's Stages as one versioned jsonb document; every AI change, manual edit and revert makes a new version (ADR-0018).

**Clarifying question**:
A question the interpreter asks the coach instead of guessing something that changes who, what, where or in what order (ADR-0019). A Drill waiting for answers is in status NEEDS_INPUT.

## Sample data

The canonical example data for mockups, prototypes, and seed data (see [Seed data content](https://github.com/cyrilleulmi/spongecoach/issues/10)). Use this — not placeholder names — whenever a Line/Player/Skill/Development goal example is needed; a Focus example is just a short German sentence naming what a Line works on (e.g. "Spielaufbau aus der tiefen Zone"), not drawn from a catalog.

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

**Example Focus text** (each set per Line per Event, not a catalog): Spielaufbau aus der tiefen Zone, Abschlussübungen 2-auf-1, Einläufe im Überzahlspiel, Cross-Pässe unter Druck, Rebound-Kontrolle nach Pad-Abwehr

**Drill tag list** (seeded, fixed in v1): Mit Gegenspielern, Ohne Gegenspieler, Mit Goalie, Überzahl, Schiessen, Passen, Stockführung, Spielaufbau, Aufwärmen, Wiederholung spiegelverkehrt

**Example Drills** (real tactic-board photos with confirmed interpretations in [docs/drills/examples/](docs/drills/examples/)): Acht, Bresil, Halbkreis, Karussell, Langkurz, Slalom, Waschmaschine, 3v2
