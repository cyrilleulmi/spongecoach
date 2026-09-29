# Drill examples

Real tactic-board photos (Fat Pipe whiteboard, full-rink and half-rink sides) used to tune and check
the drill interpreter. One folder per Drill; photos are numbered in upload order.

Each folder also holds `animation.json`: Claude's answer from a real run of
`gradlew drillExamples`. In dev mode, `DrillExampleSeeder` loads every folder as a ready Drill
(ADR-0018). To refresh one, copy its answer from `backend/build/drill-examples/<name>.json` after a
run.

Each `expected.md` is the reference interpretation, confirmed by the coach who drew it:

- **Tags / photo relation** — what the coach would select on upload.
- **Stages** — actors and steps per Stage, in order. Positions are described relative to the rink
  as it appears in the photo, not in metres.
- **Must ask** — ambiguities the interpreter has to raise as questions instead of guessing.
- **May assume** — details it may fill in with defaults, listing them as assumptions.
- **Coach answers** — scripted replies to the *Must ask* questions. The example test feeds them
  back when the interpreter asks, then checks the finished animation.
- **Open for the coach** — drafting doubts to resolve before the file counts as a reference. Delete
  the section once answered.

A run passes when every Stage and step matches, every *Must ask* point is asked (or settled by the
tags), and nothing in *Must ask* was silently guessed.

## Interpretation rules

Confirmed by the coach while writing these examples. They go into the interpreter's prompt.

- **Pass direction.** Passes normally carry an arrowhead, but it's sometimes forgotten. Without
  one, work out the direction from the ball: only whoever has it can pass. List it as an
  assumption; ask only if the ball doesn't settle it.
- **Who runs which line** also follows from the ball: whoever just passed usually runs on to where
  the next pass arrives.
- **No goalie drawn** means the goalie doesn't matter: many drills work either way. Don't ask;
  animate an empty goal.
- **Solid line vs wavy line.** A solid line is a run, with or without the ball. A wavy line is a
  dribble: the player actively moves the ball left and right with the stick. Who has the ball
  doesn't depend on the line: a player has it if they start with it or received a pass, until
  they pass or shoot.
- **Repetition.** The animation loops one run. Either it replays: reset to the start and play
  again. Or it loops seamlessly: players move into their next roles (e.g. the middle player moves
  out and the last passer comes in), then the next repetition starts. When the drawing doesn't
  show how repetitions connect, guess if confident (e.g. a queue drill where the next player
  simply steps up) and list it as an assumption; otherwise ask, proposing a rotation.
- **"Wiederholung spiegelverkehrt"** (repeat mirrored, a tag): after the drawn run, the drill
  repeats from the other side, mirrored across the rink's long axis (left ↔ right). Players swap
  roles as the drill implies; e.g. in `langkurz` the passer goes to the other side and waits for
  her turn, and the receiver starts the mirrored run. The loop alternates sides.
- **Game situations** (e.g. 3 v 2) have no fixed order. Ask with a concrete proposal ("Should the
  attackers pass among themselves in a random order while the defenders just move around?"). Once
  the coach confirms, make up a plausible order. Not every drill has one exact animation; show
  what the drill is for.
- **Things that aren't drawn** within one run are left out of the animation instead of invented.
  How one repetition leads into the next is asked (see Repetition).
