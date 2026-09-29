---
status: accepted
---

# Claude interprets drill sketches as a background job, and asks instead of guessing

Turning a photo of a hand-drawn drill into an animation (ADR-0018) needs a vision model that
understands floorball. Drawings are messy: half-erased residue from earlier drills, glare,
rotated boards, missing arrowheads, and symbols whose meaning depends on the drill (an O may be a
defender or a cone). A wrong animation that looks confident is worse than none, so the model must
ask the coach when a drawing is ambiguous.

**Model and SDK.** `claude-opus-5` through the official `anthropic-java` SDK. It replaces the raw
`HttpClient` of the earlier Übungen chat proof of concept, which this decision removes. The
settings:

- Adaptive thinking, effort `high`.
- Server-side `fallbacks: "default"`.
- A refusal is treated as a failed job with a readable error.

The API key comes from `ANTHROPIC_API_KEY` (in dev, `backend/.env`, which git ignores). Without it,
every Drill job fails with a readable error on the Drill, and the rest of the app works as before.

**One interface, two implementations.** `DrillInterpreter.interpret(input)` takes the sketches,
notes, tags, relation and the conversation so far; a correction is just a longer conversation.
`AnthropicDrillInterpreter` is the real one. `StubDrillInterpreter` answers as the scenario's
`Given` steps tell it (ask first, an unplayable script, a refusal), so `drills.feature` runs without
network or cost (ADR-0013).

**Photos arrive upright.** Phones store a portrait photo as landscape pixels plus an EXIF
rotation. The browser applies it when it shrinks the photo before upload (`createImageBitmap`), so
the model sees the board the right way up. In testing, sideways photos made the model misread
arrows and numbers it reads easily when they are upright.

**Two passes in one request.** The model first *reads*: for each photo it lists every symbol with
its kind, colour, position on the board, sequence number, confidence, and the photo's
orientation. Then it *builds* the Stages from that list. The symbol list is stored alongside the
result, and the UI shows it next to each photo, so a coach can see what was misread.

**The answer format.** The answer is either:

- `{status: "NEEDS_INPUT", questions[]}`: at most 5 questions, most important first. Each
  question points at the photo and symbols it's about, and may offer options.
- `{status: "READY", script, changeSummary}`.

The JSON schema (`drill/interpretation.schema.json`) is part of the cached system prompt rather
than a structured-output format: the API rejects it as `output_config.format` ("the compiled
grammar is too large"). So the answer is parsed leniently. An answer that isn't JSON at all gets
one retry inside the interpreter. The server then validates a `READY` script (ADR-0018). If it
isn't playable, the job retries once, feeding the errors back; a second failure fails the job.

**Ask, don't guess.** The prompt draws a hard line:

- **Must ask about:**
  - The role of a symbol (defender or cone, and whether an unclear circle is the goalie).
  - Which side an Actor is on, when neither colour nor shape nor role shows it.
  - Progression versus continuation.
  - Which Actor in one photo is which in the next.
  - The order of Steps when neither numbers nor the ball settle it.
  - A drawing that contradicts itself.
- **May assume** (listed as assumptions):
  - Speeds, curve shapes and small timing gaps.
  - Movement inside a `roam`.
  - Pass direction when the ball settles it.
  - The repetition, when the model is confident.
  - No goalie drawn means the goalie doesn't matter.
- **Game situations** (a 3 v 2 with no fixed order) get a question with a concrete proposal
  first. Once the coach confirms, the model makes up a plausible order that shows what the drill
  is for.

The conventions come from `docs/drills/examples/README.md`, where the coach confirmed them. The
prompt carries the rules, not the eight worked examples, so checking against the examples measures
how well it generalises rather than whether it memorised them.

**Tags and notes are hints.** They go into the prompt as the coach's intent. If the drawing
contradicts them, the model asks.

**Background jobs.** An interpretation takes one to three minutes (a complex drill with a
question round, longer), so every model call runs off the request thread:

- `POST` returns 202, and the Drill's `status` moves through PENDING → NEEDS_INPUT or READY, or
  FAILED with an `error`.
- The frontend polls `GET /api/drills/{id}` every 2 s.
- Only one job may run per Drill; another request while one is running gets 409 `drill_busy`.
- Jobs live in memory. On startup, any Drill still PENDING is set to FAILED, so a restart never
  leaves one stuck.

**The conversation.** Answers to questions and correction-chat messages are stored as
`drill_message` rows. Each request sends the whole conversation, append-only, with the system
prompt and the photos as the unchanging start:

- **Caching.** That start is cached (`cache_control`), so a chat turn with 12 photos pays mostly
  cache reads.
- **Asking back.** In the correction chat the model may also answer with questions rather than a
  change.
- **Undo.** Every change it makes is a new script version with a change summary, so "Rückgängig"
  is a revert.

**Checking the prompt.** `gradlew drillExamples` (one example: `-Pexample=bresil`) runs every
example in `docs/drills/examples/` against the real API: it downscales the photos the way the
browser does, answers questions with the scripted coach answers, and writes a report per example
to `build/drill-examples` to compare with `expected.md`. It costs money (about 10–15 calls for all
eight), so it is run by hand when the prompt changes, and is not part of `test` or `verify.ps1`.
