---
status: accepted
---

# A Drill can be drawn by hand, on any device, and every photo and Drill can be removed and restored

ADR-0018 assumed a Drill starts as photos and Claude turns them into an animation. A coach (or a
Player) also wants to build one from nothing, with no photo and no paid API call, on a phone at the
rink. The script format was already editor-shaped; what was missing was a way to create, and a few
places where photos were hardwired.

**A drawn Drill.** A Drill with no photos, created by `POST /api/drills/drawn` with a name, tags and
a finished script. It is READY at once as version 1 (source `EDIT`) and no interpreter job runs. The
Drill is only created on the first save, so an abandoned drawing leaves nothing behind; until then
the animator keeps a draft in the browser. `sketch_relation` is `not null` and stays so: a drawn
Drill gets `PROGRESSION`, meaning nothing. No migration for this.

**A Step drawn by hand names sketch 0.** `sketch` on a Step was "the photo it is drawn on", 1 to
*n*. 0 now means "on no photo". The validator accepts 0 on every Step of every Drill, so a Step
added by hand to a photo Drill works too. A Stage's `sketches` list still holds only real photo
positions. The AI schema keeps saying "1-based" except for a Step it adds itself in a chat, where it
uses 0.

**Chat still works on a Drill without photos.** It sends no images, tells the model there are no
photos, and puts the cache point on the intro. Everything the chat can change must also be doable by
hand, so the chat is a shortcut, never the only way. A drawn Drill has nothing to *interpret*, so
`retry` on one is refused (400).

**A Step can start part-way through another.** `afterEdge` gets a third value, `DURING`, with
`afterFraction` (0 < f < 1): the Step starts `f` of the way through its anchor Step, plus `delay`.
This is how a pass or shot happens *while* someone runs, not only before or after. It stays on the
same spot of the run when a speed changes, which "start + delay" would not. The scheduler, the
validator (a fraction strictly between 0 and 1, and an anchor), the schema and the prompt all know
it; the prompt tells the model to use it when an arrow starts on the middle of a run arrow.

A pass to a moving receiver ends where the receiver will be when the ball lands. The animator works
that out with a few fixed-point iterations and re-snaps it when the passer's or receiver's Steps
change. Only passes the animator snapped are re-snapped (a flag that lives in the editor, not in the
script); a pass from the AI is left as it is.

**Photos are removed softly and positions are never reused.** `drill_sketch.deleted_at` (ADR-0004).
Script versions refer to photos by position, and a sketch URL is cached for good, so a removed photo
keeps its row and its position; a new photo takes the highest position ever used plus one. The
validator checks a script's photo references against every position ever used, removed ones
included, so reverting to an old version never breaks. The interpreter is only given active photos.
A Drill holds at most 12 active photos; restoring a photo past that is refused. Adding a photo
starts no job: the coach may ask Claude, through the chat, to work the new photos in.

**Drills are restorable.** `GET /api/drills/deleted` and `POST /api/drills/{id}/restore` follow what
Lines do. The example seeder still never re-adds a deleted example by name; restoring brings the
original back.

**Who may do what.** ADR-0018 made every Drill write a Coach's because each could call the paid API.
Only the writes that do are still a Coach's: uploading photos to be interpreted, answering,
chatting and retrying. Drawing a Drill, editing its script, reverting, renaming and tagging, adding,
removing and restoring photos, and deleting and restoring a Drill are any team member's, Player
included (`Access.requireTeamMember`). With one Team every Player is a member; a Player of another
Team would be refused, but no scenario can create one until Teams can be.

**The animator.** One editor for drawn and photographed Drills, mobile first: the rink in portrait
at full width without zoom, a bottom tool bar and bottom sheets instead of side panels, touch
targets of at least 44 px, paths drawn freehand (simplified to a few waypoints) or tapped point by
point, and a timeline whose bars are dragged to re-time Steps. Its logic lives in a pure module,
`drill-authoring.ts`, tested without a DOM like `paint-engine.ts` and `drill-schedule.ts`.

**Out of scope for now.** Zoom and pan on the rink, dragging Stages to reorder them (↑/↓ buttons
instead), and a cost limit on chat.
