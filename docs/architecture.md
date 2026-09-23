# Architecture

How SpongeCoach is put together. The domain language lives in [CONTEXT.md](../CONTEXT.md), the
decisions in [adr/](adr/), the behaviour in [spec/](spec/). This file is the map between them.

## Shape

```
frontend/  Angular 20 standalone components, signals, no state library.
           Dev server proxies /api -> localhost:8080 (proxy.conf.json).
backend/   Quarkus + Hibernate Panache (active-record). REST resources map straight to
           entities; there is no service layer, because there is no logic that outlives a
           request. Flyway owns the schema (ADR-0003).
Postgres   docker-compose for dev; Quarkus Dev Services (Testcontainers) for tests.
```

Single hardcoded Team, no auth in v1. UUID primary keys everywhere (ADR-0002).

## Backend layers

- `api/` — JAX-RS resources, one per aggregate. Validation is inline and throws
  `BadRequestException` / `ConflictException` / `NotFoundException`; `ApiExceptionMapper` turns
  those into a uniform `{error, message}` envelope with status 400 / 409 / 404.
- `api/dto/` — records with static `from(entity)` factories. Read DTOs denormalise names
  (`FocusAttachmentDto` carries `lineName` alongside the free-text `focus`) so the timeline is one
  call, no N+1.
- `domain/` — Panache entities with public fields and static finders. Join tables that carry a
  payload are entities (`LineSkill`, `LineFocusEvent`, `EventAttendance`); join tables that do not
  are `@ManyToMany` (`line_player`, `event_line`).

## Data model

```
Team 1─* Line ─*─* Player            (line_player: a Player may be on several Lines, ADR-0008)
         Line ─*─* DevelopmentGoal   (line_development_goal)
Player ─*── PlayerSkillRating ──* PlayerSkill   payload: rating 0-100, overwritten (ADR-0015)
Player ─*─* PlayerDevelopmentGoal               (player_player_development_goal)
                                     Player lists are separate from the Line Skill/goal lists
Player 1──0..1 PlayerAvatar          (player_avatar: painted PNG bytes; version = player.avatar_updated_at, ADR-0016)
         Line ─*── LineSkill ──* Skill      payload: rating 0-100, overwritten, no history
Team 1─* Iteration 1─* Event
         Event ──* EventType
         Event ─*─* Line             (event_line: SNAPSHOT of attending Lines, ADR-0012)
         Event ──* EventLinePlayer   (event_line_player: snapshot of each attending Line's roster)
         Event ──* EventAttendance   (one answer per (Event, Player): PENDING|ATTENDING|DECLINED)
         Event ──* LineFocusEvent    (at most one per (Event, Line); `focus` is free text, ADR-0014)
```

Soft delete (`deleted_at`) on Line, Skill, DevelopmentGoal, PlayerSkill, PlayerDevelopmentGoal (ADR-0004). `Line.color` is assigned
once at creation from a fixed 8-color dial palette, so a Line's color survives its own deletion on
historic Event dials.

### The two snapshots that make history work (ADR-0012)

Creating an Event freezes two things, and nothing recomputes them afterwards:

1. **`event_line`** — every *currently active* Line. A Line created later never joins retroactively;
   a Line deleted later still shows on the Events it attended.
2. **`event_line_player`** + **`event_attendance`** — each attending Line's roster at that moment,
   every Player defaulted to `PENDING`.

A Line roster edit reaches the snapshots of Events with `scheduled_on >= now` only
(`LineResource#syncAttendanceForRosterChange`). Done Events are history and are never touched. A
Player dropped from one of two attending Lines keeps their single answer through the other.

## API

| Method | Path | Notes |
|---|---|---|
| GET | `/api/lines` | active Lines, name order, with `playerCount` and `color` |
| GET | `/api/lines/{id}` | roster, Skill ratings and Development goals inline |
| POST | `/api/lines` | `{name}`; assigns the next palette color |
| PUT | `/api/lines/{id}` | `{name?, playerIds?, developmentGoalIds?}` — each list replaces wholesale; a roster change syncs upcoming Events' attendance |
| DELETE | `/api/lines/{id}` | soft delete |
| GET | `/api/lines/deleted` | most recently deleted first |
| POST | `/api/lines/{id}/restore` | |
| GET/PUT/DELETE | `/api/lines/{id}/skills[/{skillId}]` | `PUT {rating}` 0-100, upserts the association |
| GET | `/api/players` | the Team pool, each with its active `lines`; seed-only in v1 |
| GET | `/api/players/{id}` | `lines`, Player skill `skills` (with rating) and `developmentGoals` inline; 404 if unknown |
| PUT | `/api/players/{id}` | `{developmentGoalIds?}` — replaces the set wholesale |
| GET/PUT/DELETE | `/api/players/{id}/skills[/{skillId}]` | `PUT {rating}` 0-100, upserts; Player skills only, a Line Skill id is 404 |
| GET/POST/PUT | `/api/player-skills`, `/api/player-development-goals` | the Player lists, with a color (ADR-0015) |
| GET/PUT/DELETE | `/api/players/{id}/avatar` | the painted Avatar as `image/png`; `PUT` takes a square PNG ≤ 512 px / 200 KB, returns the Player detail; `GET ?v=<avatarVersion>` is cached immutable; every Player read (list, detail, Line roster, Event attendance) carries `avatarVersion`, null for initials (ADR-0016) |
| GET | `/api/event-types` | seeded Training / Match |
| GET/POST/PUT | `/api/skills`, `/api/development-goals` | shared catalogs with a color |
| GET | `/api/iterations` | **the timeline**: Iterations by position, Events nested by datetime, each with `focusAttachments`, `lines`, `attendance` |
| POST | `/api/iterations` | `{name, position?, events?}` — Events may be nested |
| PUT/DELETE | `/api/iterations/{id}` | delete cascades to its Events |
| POST | `/api/iterations/{id}/events` | `{eventTypeId, name?, scheduledOn}`; takes the attendance snapshot |
| PUT/DELETE | `/api/iterations/{it}/events/{ev}` | reschedule / rename / delete |
| PUT | `/api/events/{id}` | `{eventTypeId?, name?, scheduledOn?, focusAttachments?}` — `focusAttachments` is `[{lineId, focus}]` free text; a non-null value **replaces the whole set** |
| PUT | `/api/events/{id}/attendance/{playerId}` | `{status, declineMessage?}`; the message is kept only while `DECLINED` |

`scheduledOn` is mandatory and unique within its Iteration (ADR-0011); a collision returns **409
`scheduling_conflict`**. The same datetime in two different Iterations is fine.

## Frontend

Four routes: `/team` (default), `/lines`, `/players`, `/players/:id`. The header nav reads
Team-Übersicht · Blöcke · Spieler.

- **`/lines`** — one Line at a time; `?line=<id>` preselects one (unknown id → first Line), which
  is how a Line badge elsewhere jumps here. Roster rows link to the player screen. Ratings are optimistic with rollback; every other association
  edit sends the full id array and refetches. The roster dialog stages changes and commits once on
  "Fertig". New Skills / Development goals are created from here and auto-associated (ADR-0009).
  Focus is not managed here (ADR-0014) — it's set per Event from `/team`.
- **`/team`** — the Iteration timeline. Each Event is a dial with one slice per *snapshotted*
  attending Line, lit in that Line's color when the Line has a Focus set — so a dial reads as
  "how much of this session is planned". A Line's Focus is a plain text field in the event detail
  panel; "same focus again" copies that Line's most recent earlier Focus text into it (ADR-0014).
  Roster icons under the dial show each Player's answer (not linked — too small a target);
  attendance rows in the event detail link to the player screen. The **next** Event is the first, in
  Iteration-then-datetime order, whose datetime has not passed.
- **`/players`** — every Player with their Avatar (`PlayerAvatar`: the painted image, or initials
  when there is none or it fails to load), name and clickable Line badges (`LineBadge`).
  **`/players/:id`** — that Player's Player skill Ratings (optimistic, like Lines) and Player
  development goals (full-set save); new ones are created here and associated straight away. A 404
  renders "Spieler nicht gefunden". Clicking the Avatar opens `AvatarPainter`, a modal canvas
  (brush, spray, fill, eraser, undo) with a circle guide over the saved square; its pixel math lives
  in `paint-engine.ts` so it is testable without a canvas (ADR-0016).
- **Read-only past Events** are a frontend default, not a backend rule (ADR-0012): an Event whose
  datetime has passed renders disabled, and the coach can lift the lock per Event with "Trotzdem
  bearbeiten". The lock returns when another Event is selected.

UI copy is German; every domain term in code, API and docs stays English ([glossary.md](glossary.md)).

## Verifying

`.\verify.ps1` runs all of it: backend, spec coverage, frontend Jest, Playwright e2e.

The backend suite **is** the specification. `docs/spec/*.feature` (except `ui.feature`) is executed
by Cucumber through `SpecTest`, one Quarkus boot, black-box over the HTTP API — there are no
separate backend unit tests of the resource layer. `ui.feature` is proven by the frontend suites and
tied to them by `// spec:` marker comments. ADR-0013 records why the two layers differ; see
[spec/README.md](spec/README.md) for how to add a scenario or a feature file.

## Known gaps

- Playwright e2e stubs the API in the browser; no e2e runs against the real backend.
- Backend does not enforce read-only past Events — the frontend does, deliberately.
- `Line.count()` drives the palette index and counts soft-deleted Lines, so colors can repeat
  before all eight are used.
