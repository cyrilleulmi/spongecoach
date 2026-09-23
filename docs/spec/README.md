# Executable specification

This folder is the **behavioural source of truth** for SpongeCoach: what the app does, in domain
language, independent of Java/TypeScript. `CONTEXT.md` defines the *words*; `docs/adr/` records the
*decisions*; these `.feature` files record the *behaviour*.

The six backend feature files are **run** by Cucumber against the real HTTP API. `ui.feature` is
not — it is proven by the frontend suites and traced by marker comment. ADR-0013 records why.

## Layers

| File | Layer | Proven by |
|---|---|---|
| `lines.feature` | Line lifecycle, roster, ratings, catalog associations | `LineSteps.java` |
| `catalogs.feature` | Skill / Development goal / Focus catalogs | `CatalogSteps.java` |
| `timeline.feature` | Iterations, Events, scheduling | `TimelineSteps.java` |
| `focus-planning.feature` | The Line-Focus-Event link | `FocusSteps.java` |
| `attendance.feature` | Attendance snapshot and answers | `AttendanceSteps.java` |
| `players.feature` | Player list/detail, Player ratings and development goals | `PlayerSteps.java` |
| `ui.feature` | Screen behaviour a coach sees | `frontend/src/app/**/*.spec.ts`, `frontend/e2e/*.spec.ts` |

Backend scenarios are black-box over the HTTP API: a `Given` is fixture state, a `When` is one
request, a `Then` is the response or a follow-up read. UI scenarios are about what the coach sees
and clicks, never about component internals.

## How a backend scenario runs

`backend/src/test/java/com/spongecoach/spec/SpecTest.java` is the runner. It boots Quarkus once and
executes the feature files listed in its `@CucumberOptions`, with step definitions in
`com.spongecoach.spec.steps`:

| Class | Holds |
|---|---|
| `steps/SharedSteps` | The response outcomes many scenarios end on (bad request, not found, conflict) |
| `steps/*Steps` | One class per feature file, named after it |
| `support/ScenarioWorld` | What the scenario's spoken names refer to, the last response, the undo stack |
| `support/Fixtures` | Creates what a `Given` describes, and queues its removal |
| `support/Hooks` | Runs the undo stack after each scenario |

Scenarios name fixtures the way `CONTEXT.md` does — the Line "Kiwi", the Skill "Passgenauigkeit".
Every scenario in the run shares one database, so `Fixtures` writes each row under a uniquified name
and registers it in the `ScenarioWorld` against the spoken one. Steps resolve through the world, so
the feature file stays readable while the rows stay isolated. **Never create a row directly in a
step** — go through `Fixtures`, or the scenario will leak state into later ones.

### Adding a backend feature file

Register it in **two** places, or it will silently never run:

1. `SpecTest`'s `features` list.
2. `CUCUMBER_FEATURES` in `scripts/check-spec-coverage.mjs`.

The checker fails on a feature file missing from either.

## How a UI scenario is traced

`ui.feature` keeps the marker protocol. Each scenario carries a stable id tag:

```gherkin
@spec:ui.event-readonly-when-done
Scenario: An Event whose datetime has passed cannot be edited
```

and the test that proves it carries the same id in a marker comment on the line above it:

```ts
// spec: ui.event-readonly-when-done
it('disables editing for a past event', () => { ... });
```

## What the checker checks

`node scripts/check-spec-coverage.mjs` (run by `verify.ps1`, after the backend stage) reports:

- **not executed** — a backend scenario Cucumber never ran, because it is missing from `SpecTest` or
  was filtered out by a tag. Always a failure. An *undefined* step already fails the backend suite
  on its own; this catches the scenario that was quietly skipped instead.
- **failed** — a backend scenario that ran and did not pass.
- **unchecked feature file** — a `.feature` that is neither run nor scanned.
- **uncovered** — a `ui.feature` scenario with no test marker.
- **orphaned** — a test marker pointing at a scenario id that no longer exists. Always a failure: it
  means the spec was renamed or deleted out from under a test.

Scenarios tagged `@unverified` describe behaviour v1 does not implement. They are skipped by the
runner, expected to have no test, and listed separately.

The backend half needs the report from a backend run (`backend/build/cucumber-report.json`). With
Docker down, `verify.ps1` skips the backend stage and the checker says so rather than failing.

## Writing rules

- One rule per scenario. If a scenario needs "and also", it is two scenarios.
- Use `CONTEXT.md` vocabulary exactly — Line, Focus, Iteration, Event, Development goal, Rating.
  Never Block, squad, appointment, or Goal-meaning-development-goal.
- Scenario ids are `<area>.<kebab-rule>` and are permanent. Renaming one means updating its marker
  (`ui.feature`) — the id is what the checker reports against.
- Every step must be executable: no step may rely on context the scenario has not stated. Prefer
  reusing an existing step's wording over inventing a near-duplicate.
- When behaviour changes, the feature file changes **first**, in the same commit as the code.
