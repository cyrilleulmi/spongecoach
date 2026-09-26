## Agent skills

### Issue tracker

Issues live on GitHub (`cyrilleulmi/spongecoach`), via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Default label vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.

### Behaviour spec

`docs/spec/*.feature` is the behavioural source of truth, and `docs/architecture.md` the map of how
the pieces fit. When behaviour changes, change the feature file in the same commit as the code.

The seven backend feature files are **executed** by Cucumber (`SpecTest`) and are the backend test
suite — there are no separate resource-layer tests. Changing backend behaviour means editing the
scenario and its step definition in `com.spongecoach.spec.steps`; never create a fixture row in a
step except through `Fixtures`. `ui.feature` is the exception: it stays traced to the frontend
suites by `// spec: <id>` marker comments. See `docs/spec/README.md` and ADR-0013.

### Permissions

`docs/permissions.md` is the permissions matrix: what SysAdmin, Coach and Player may call, and which
`Access` check enforces it (ADR-0017). **Read it before implementing any request that adds or
changes an endpoint or an editable control, and extend it in the same change.** That means a row for
every new or changed endpoint, with an explicit decision for every Role, enforced through `Access`,
proven in `docs/spec/authorization.feature` (at least one allowed and one refused case), and mirrored
in `CurrentUserService` / the UI. If a Role's access isn't obvious from the request, ask. The default
is: Coach yes within their Team, SysAdmin yes, and a Player only for their own Player, Lines and
answers.

### Verifying

`.\verify.ps1` from the repo root runs spec traceability, backend, frontend and e2e, and prints one
verdict. Use it before reporting work as done. Docker must be running for the backend stage.

### Wayfinder

Current wayfinder map: [SpongeCoach: floorball team collaboration MVP](https://github.com/cyrilleulmi/spongecoach/issues/1).

## Git

Always `git add` the necessary changes once a change is finished, so the working tree reflects completed work. This does not authorize committing — only staging; still ask before creating commits.

## Planning

- Be extremely concise. Sacrifice grammar for concision — plans are scannable, not prose.
- End every plan with a list of unresolved questions: edge cases, error handling, unclear requirements — ask before proceeding.
- End every plan with a numbered list of concrete steps, as the last thing in the plan, so it's visible without scrolling up.
