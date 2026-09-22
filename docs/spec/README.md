# Executable-backed specification

This folder is the **behavioural source of truth** for SpongeCoach: what the app does, in domain
language, independent of Java/TypeScript. `CONTEXT.md` defines the *words*; `docs/adr/` records the
*decisions*; these `.feature` files record the *behaviour*.

Written in Gherkin because the behaviour here is rule-shaped ("a Focus per Line per Event", "a
collision is rejected") and the existing test names already read as Given/When/Then. The feature
files are not run by a Cucumber runtime — the repo has no step-definition layer and does not need
one. Instead every scenario is **traced** to the real automated test that proves it, and a checker
fails if that link breaks.

## How a scenario is traced

Each scenario carries a stable id tag:

```gherkin
@spec:timeline.slot-collision
Scenario: Two Events in one Iteration cannot share a datetime
```

The test that proves it carries the same id in a marker comment on the line above it:

```java
// spec: timeline.slot-collision
@Test
void whenAddingAnEventAtAnAlreadyUsedSlot_thenReturnsConflict() { ... }
```

```ts
// spec: ui.event-readonly-when-done
it('disables editing for a past event', () => { ... });
```

`node scripts/check-spec-coverage.mjs` (also run by `verify.ps1`) reports:

- **uncovered** — a scenario with no test marker. Either the behaviour is unverified, or the marker
  was forgotten. Not automatically a failure: scenarios tagged `@unverified` are expected to have no
  test and are listed separately.
- **orphaned** — a test marker pointing at a scenario id that no longer exists. Always a failure:
  it means the spec was renamed or deleted out from under a test.

## Layers

| File | Layer | Proven by |
|---|---|---|
| `lines.feature` | Line lifecycle, roster, ratings, catalog associations | `backend/src/test/.../LineResourceTest.java` |
| `catalogs.feature` | Skill / Development goal / Focus catalogs | `CatalogResourceTest.java` |
| `timeline.feature` | Iterations, Events, scheduling | `IterationResourceTest.java`, `EventResourceTest.java` |
| `focus-planning.feature` | The Line-Focus-Event link | `EventResourceTest.java`, `IterationResourceTest.java` |
| `attendance.feature` | Attendance snapshot and answers | `EventResourceTest.java`, `LineResourceTest.java` |
| `ui.feature` | Screen behaviour a coach sees | `frontend/src/app/**/*.spec.ts`, `frontend/e2e/*.spec.ts` |

Backend scenarios are black-box over the HTTP API: a `Given` is fixture state, a `When` is one
request, a `Then` is the response or a follow-up read. UI scenarios are about what the coach sees
and clicks, never about component internals.

## Writing rules

- One rule per scenario. If a scenario needs "and also", it is two scenarios.
- Use `CONTEXT.md` vocabulary exactly — Line, Focus, Iteration, Event, Development goal, Rating.
  Never Block, squad, appointment, or Goal-meaning-development-goal.
- Scenario ids are `<area>.<kebab-rule>` and are permanent. Renaming one means updating its marker.
- When behaviour changes, the feature file changes **first**, in the same commit as the code.
