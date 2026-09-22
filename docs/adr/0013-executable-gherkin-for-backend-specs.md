---
status: accepted
---

# Executable Gherkin: the backend spec is run, not traced

`docs/spec/*.feature` was introduced as documentation that *described* behaviour in domain language,
with each scenario tied to the JUnit test that proved it by a `// spec: <id>` marker comment and a
checker (`scripts/check-spec-coverage.mjs`) that failed if a link broke. That bought traceability but
not truth: the checker could only verify that *a* test claimed a scenario, never that the test
asserted what the scenario said. The prose and the assertions drifted independently, and several
scenarios turned out to promise more than their test checked — `catalogs.recolor-skill` said the new
colour is "seen by every Line associated with it" while the test only read the PUT response;
`lines.create` described a palette colour "kept for the rest of its life" that nothing asserted at
all.

**The five backend feature files are now executed by Cucumber-JVM.** They are the backend test
suite: `backend/src/test/java/com/spongecoach/spec/SpecTest.java` runs them through
[quarkus-cucumber](https://github.com/quarkiverse/quarkus-cucumber), black-box over the HTTP API,
and the four `*ResourceTest` classes they replace (69 `@Test` methods, ~1500 lines) are deleted. An
undefined or failing step now fails the build, so traceability is structural rather than asserted by
a side-channel comment.

**The feature files stay in `docs/spec/`, not under `src/test/resources/`.** They are documentation
first, shared with `CONTEXT.md` and the ADRs, and a coach-readable folder of behaviour is worth more
than the convention of colocating fixtures with the code. `SpecTest` reaches them by relative path.
The cost is that a new feature file must be registered in two places — `SpecTest` and the checker —
so the checker fails on any `.feature` that is neither run nor scanned.

**`ui.feature` keeps the marker protocol.** Migrating it was considered and rejected. Much of it is
component-level behaviour that Given/When/Then cannot express honestly — an optimistic rating that
"updates before the server answers, and rolls back on failure" is a statement about component state,
not about anything an HTTP request or a browser click can observe cleanly. Running it under
playwright-bdd would have meant either weakening those scenarios to what e2e can see, or forcing ~51
Jest `it()` blocks into scenario prose. Both trade real coverage for uniformity. The result is two
mechanisms in one repo, which is a genuine cost: `docs/spec/README.md` states it plainly so the
inconsistency is not "fixed" by mistake.

**What the checker does now.** Cucumber fails on an undefined step but says nothing about a scenario
that was never *reached* — one dropped from `SpecTest`'s feature list, or filtered out by a tag. So
the checker reads the run's `cucumber-report.json` and fails on any scenario it cannot find there,
keeps the marker scan for `ui.feature`, and still fails on orphaned markers. `verify.ps1` runs the
spec stage after the backend stage for that reason; with Docker down the backend half degrades to a
reported skip rather than a false pass.

**Isolation moved from per-class to per-scenario.** All scenarios share one Quarkus boot and one
database, where each old test class had its own `@BeforeEach`/`@AfterEach` cycle. `ScenarioWorld`
(`@ScenarioScope`) holds the scenario's fixtures, the last response and an undo stack that `Hooks`
unwinds afterwards; `Fixtures` writes every row under a uniquified name and registers it against the
name the scenario spoke, so the prose can say `the Line "Kiwi"` while the row stays unique. The risk
this accepts is a leaked fixture surfacing as a puzzling failure in an unrelated listing scenario
later in the run, which is why no step may write a row except through `Fixtures`.
