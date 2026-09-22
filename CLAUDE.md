## Agent skills

### Issue tracker

Issues live on GitHub (`cyrilleulmi/spongecoach`), via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Default label vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.

### Behaviour spec

`docs/spec/*.feature` is the behavioural source of truth, and `docs/architecture.md` the map of how
the pieces fit. When behaviour changes, change the feature file in the same commit and keep its
`// spec: <id>` test marker pointing at the test that proves it (see `docs/spec/README.md`).

### Verifying

`.\verify.ps1` from the repo root runs spec traceability, backend, frontend and e2e, and prints one
verdict. Use it before reporting work as done. Docker must be running for the backend stage.

### Wayfinder

Current wayfinder map: [SpongeCoach: floorball team collaboration MVP](https://github.com/cyrilleulmi/spongecoach/issues/1).

## Git

Always `git add` the necessary changes once a change is finished, so the working tree reflects completed work. This does not authorize committing — only staging; still ask before creating commits.
