---
status: accepted
---

# A Drill is animated from photos of a tactic board, and its animation is one jsonb document

Coaches plan drills on a physical tactic board (a Fat Pipe whiteboard with a full-rink and a
half-rink side) and explain them to the team by drawing. We want the drawing to become an
animation: Actors moving, passing and shooting on a small-court rink. The coach then corrects it,
by hand or by chat.

**What a coach uploads.** A **Drill** has a name, up to 12 ordered **Drill sketches** (photos,
each with an optional note), **Drill tags** from a fixed seeded list, and a **sketch relation**:
progression, continuous or mixed. Photos are downscaled in the browser to at most 1568 px on the
long edge (what the vision model uses anyway) and stored as JPEG `bytea` in `drill_sketch`, for the
same reasons Avatars are stored in the database (ADR-0016).

**What gets animated.** A Drill has one or more **Stages**. A Stage is one continuous run. Photos
that are a *progression* (1 v G, then 1 v 1, then 2 v 1) each start a new Stage. Photos that are a
*continuation* (the next phase of the same run) extend the current one. Every Step records which
photo it came from.

A Stage holds:

- **Actors** — player, goalie or coach figures, each on side A, B or neutral. An Actor is not a
  Player: drills are about positions, not about who in the Team plays them. Actor ids stay stable
  across Stages, so "the attacker" in Stage 3 is the one from Stage 2.
- **Parts** — what an Actor does in one run (the passer, the middle player). Steps refer to Parts,
  not Actors, so that a seamless loop can hand the Parts to different Actors on the next
  repetition.
- **Props** — cones, poles, small goals.
- **Steps** — `run`, `dribble`, `pass`, `shot`, `roam` or `wait`, each with a path in rink metres,
  a speed, and an order given as "after Step X plus a delay" rather than as clock times, so that
  changing one speed moves everything after it.
  - Who has the ball is tracked separately from the Step type: a `run` may carry the ball.
  - A `dribble` is active stickhandling and is drawn with the ball moving left and right.
  - A `roam` is loose movement inside an area: the curved double-headed arrows, which the coach
    doesn't define precisely.
- **Repetition** — the animation loops one run.
  - `replay` resets and plays again.
  - `seamless` plays transition moves that put Actors into their next Parts, then carries on.
  - `mirrored` (the "Wiederholung spiegelverkehrt" tag) repeats the run from the other side,
    reflected across the rink's long axis.

**One jsonb document, not tables.** The Stages of a Drill are stored as a single `jsonb` document
in `drill_script_version`, one row per version (source: `ai`, `edit` or `revert`). We chose this
over normalised tables because:

- Nothing ever queries across Steps.
- The document is exactly what the model produces and what the editor saves.
- Keeping versions as whole documents makes undo and revert trivial.

The server validates each document before storing it: every id it refers to exists, every
position lies inside the rink, speeds are positive, the step order has no cycle, and Actor ids
stay consistent across Stages. The document's shape is the `DrillScript` record tree; its JSON
schema for the model, `drill/interpretation.schema.json`, is kept next to it by hand (ADR-0019).

**The rink.** Only the Swiss small court. Full or half, with the dimensions as one constant. The
board's printed rink is mapped onto it; the board's own proportions are not trusted.

**Scheduling and drawing happen in the browser.** Two pure TypeScript modules (`drill-schedule.ts`
and `drill-sampler.ts`) turn the step order into times and give every position at time *t*,
including the ball, Part rotation and mirroring. They are unit-tested without a canvas, like
`paint-engine.ts` (ADR-0016). The renderer is SVG.

**Who may do what.** Every User may watch every Drill (ADR-0017). Creating, editing, chatting
about and deleting a Drill was Coach-only (`requireCoach`), because each of those can call a paid
API. Deletion is soft (`deleted_at`, ADR-0004). *Amended by ADR-0020:* only the writes that start an
interpreter job stay a Coach's; drawing, hand edits, photos, deleting and restoring are any team
member's.

**Examples in dev.** In the dev profile, `DrillExampleSeeder` loads the example Drills from
`docs/drills/examples` on startup: the photos, the tags and photo relation from each
`expected.md`, and `animation.json`, Claude's answer from a real run of the example test. So the
app has drills to look at without spending API credits. Each is added once by name, and not again
after a coach deletes it.

**Out of scope for now.** Linking a Drill to an Event or a Focus, Coach-created tags, cost limits,
and exporting as video. (Drawing a Drill without photos, first listed here, is ADR-0020.)
