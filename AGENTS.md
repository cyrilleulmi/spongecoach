# AGENTS.md — SpongeCoach

Guidance for AI coding agents (Claude Code, etc.) working in this repository. Read this before making changes.

## What this project is

SpongeCoach is an application for smaller Swiss sports clubs and their coaches. The core domain is **member & practice management** (Persons, Clubs, Teams, and — later — practices). The long-term vision is a "Swiss army knife" of helper tools (event planning, statistics, fun games), but the core domain must stay clean and easy for the maintainer to evolve by hand.

The maintainer is a software architect. They will frequently edit the critical domain themselves. Optimize your changes for **reviewability** and **clean separation**, not for cleverness.

## Stack

| Layer | Choice |
|---|---|
| Frontend | Angular (latest stable), standalone components, signals, plain SCSS + CSS variables |
| Backend | Spring Boot (latest stable), Java 25 (LTS), Gradle (Kotlin DSL) |
| Database | Postgres 16 |
| Migrations | Liquibase (YAML changelogs under `backend/src/main/resources/db/changelog/`) |
| Deployment | Local + Docker Compose; everything must be dockerizable |
| Auth | None yet — Person is a pure domain object; anyone using the app can manage everything |

## Repo layout

```
spongecoach/
├── frontend/               # Angular app
├── backend/                # Spring Boot app
├── docker-compose.yml      # full stack
└── docker-compose.dev.yml  # postgres-only, for native dev loop
```

## Backend architecture — hexagonal, per feature

Each domain feature (`club`, `person`, `team`, future ones) lives in its own package with this shape:

```
coach/spongecoach/<feature>/
├── domain/         # pure — NO Spring, NO JPA imports here
│   ├── model/
│   └── <Feature>Repository.java        # port (interface)
├── application/    # use-case services, exceptions
└── adapter/
    ├── in/web/     # @RestController + DTOs
    └── out/persistence/   # JPA entities, Spring Data repos, port adapter, mappers
```

Hard rules:
- The `domain/` package has **zero Spring or JPA imports**. If you need a dependency there, it goes through a port.
- DTOs are explicit at the API boundary. Never serialize domain models directly.
- Identifiers are UUIDs generated in the domain.
- Liquibase owns the schema. JPA runs with `ddl-auto=validate`.
- Schema changes go in **new** Liquibase changesets — never edit a changeset that has already been applied.

## ArchUnit — keep the architecture honest

The hexagonal layout is enforced by **ArchUnit tests**, not by goodwill. There is an ArchUnit test class at `backend/src/test/java/coach/spongecoach/architecture/ArchitectureTest.java` that runs as part of `./gradlew test`. It must stay green.

Rules the ArchUnit tests enforce (extend them as the project grows — don't relax them):

- Classes in `..domain..` may **not** depend on `org.springframework..`, `jakarta.persistence..`, `org.hibernate..`, or any `..adapter..` package.
- Classes in `..application..` may depend on `..domain..` but **not** on any `..adapter..` package.
- Classes in `..adapter.in..` and `..adapter.out..` of one feature may **not** depend on the `..adapter..` packages of another feature. Cross-feature collaboration goes through application services or domain ports.
- No cyclic dependencies between feature packages (`club`, `person`, `team`, …).
- Only classes under `..adapter.in.web..` carry `@RestController` / `@Controller`.
- Only classes under `..adapter.out.persistence..` carry `@Entity` or extend Spring Data repository interfaces.
- Domain model classes are not annotated with persistence or web annotations.

When you add a new feature, add or update ArchUnit rules so the new package is covered. When a rule legitimately needs to change, change it deliberately in its own commit with a message explaining why — never quietly.

## Frontend architecture

- Standalone components, signal-based state.
- Per-feature folders under `src/app/features/` (`clubs/`, `teams/`, `persons/`, …).
- All design tokens live in `src/styles/_theme.scss` as CSS variables. Components consume variables only — **never hard-code colors, spacing, or radii**. Reskinning the app must mean editing one file.
- API access goes through typed clients in `src/app/core/api/`. The Angular dev server proxies `/api/*` to the backend.

## Commit workflow — IMPORTANT

The maintainer reviews your work commit-by-commit. Make their life easy:

1. **Commit feature-by-feature, not file-by-file and not all-at-once.**
   A "feature" here means a coherent vertical slice — e.g. "add Person domain + repository port", "wire Person REST endpoints", "add Person list page in frontend". Each commit should leave the build green and tell a story on its own.

2. **One concern per commit.** Don't mix a refactor with a feature, or a bugfix with a rename. If you find yourself writing "and" in a commit message, split it.

3. **Order commits the way a reviewer would want to read them**: domain → application → adapters → frontend. Migrations land with the code that needs them.

4. **Commit message style**: short imperative subject (≤ 70 chars), then a blank line, then a body that explains *why* if non-obvious. Examples:
   - `add Club aggregate and repository port`
   - `expose /api/clubs CRUD endpoints`
   - `add Club list page with create form`
   - `add Liquibase changeset for team_members`

5. **Do not squash before review.** Hand the maintainer the granular history.

6. **Never commit without being asked.** Stage the work, summarize what's ready, and let the maintainer say "commit it" — but when they do, follow the rules above. (This applies once the project has substance; for the initial bootstrap, follow the maintainer's lead on when to commit.)

7. **Never push, force-push, amend public commits, or rewrite history without explicit instruction.**

## Working on a new feature — checklist

When adding a new domain feature end-to-end (the typical task):

1. **Domain first**: model + value objects + repository port, with unit tests. Commit.
2. **Application**: use-case service(s) and exceptions, with unit tests against an in-memory port. Commit.
3. **Persistence adapter**: JPA entity, Spring Data repo, port implementation, mapper. New Liquibase changeset for any schema change. Commit.
4. **Web adapter**: controller, DTOs, validation, error mapping. Slice tests (`@WebMvcTest`). Commit.
5. **ArchUnit coverage**: extend `ArchitectureTest` so the new package's boundaries are enforced. Commit.
6. **Integration test**: one Testcontainers-backed end-to-end test exercising the new endpoints. Commit.
7. **Frontend API client**: typed service in `core/api/`. Commit.
8. **Frontend feature pages**: list / detail / forms under `features/<name>/`, wired into the router. Commit.

Each step leaves the build green.

## Things that need maintainer approval before you do them

- Adding a new top-level dependency (Gradle or npm).
- Introducing a new framework, library, or build tool.
- Changing the architectural shape (e.g., collapsing the hexagonal layout, changing the package convention).
- Relaxing or removing an ArchUnit rule.
- Schema changes that aren't additive.
- Any deletion or rename that crosses feature boundaries.
- Anything touching `docker-compose.yml`, Dockerfiles, or CI.

When in doubt, ask first. The cost of a 30-second clarification is nothing compared to a wrong-direction PR.

## What's intentionally out of scope right now

- Authentication and real user accounts.
- Practice management, event planning, statistics, fun games (future "Swiss army knife" features).
- CI pipeline and production deployment manifests.
- i18n (DE / FR / IT will come — keep strings extractable, but don't wire i18n yet).

## Test data conventions

SpongeCoach is a floorball (unihockey) app. All test data — in unit tests, slice tests, integration tests, and Liquibase dev seed scripts — must reflect this:

- **Club names**: use Swiss floorball club naming style — `UHC <City>` (Unihockey Club) or `SHC <City>` (Schul- und Hobby-Club). Examples: `UHC Malters`, `UHC Köniz`, `SHC Bümpliz`. Never use `FC` (football) prefixes.
- **Locations**: Swiss towns associated with real Swiss floorball clubs are preferred — Malters, Köniz, Zug, Kloten, Thun, Langnau, etc. Generic cities like Zurich or Bern are acceptable; Geneva is not (French-speaking Switzerland has little floorball tradition).
- **Team names**: use Swiss league category naming — `Herren 1`, `Damen`, `Junioren A`, `Juniorinnen B`, `Junioren C`, etc. Never use `U17` or other football-style age-group notation.
- **Person names**: any realistic names are fine (no domain constraint).

## Verification before reporting "done"

- Backend: `./gradlew test` is green, including ArchUnit and Testcontainers integration tests.
- Frontend: `npm run build` succeeds and `npm test` is green for any service you added.
- Full stack: `docker compose up --build` starts cleanly; the app is reachable on `http://localhost:4200`; the new feature works end-to-end through the UI.
- Theme sanity: changing a value in `frontend/src/styles/_theme.scss` still propagates everywhere (no hard-coded colors leaked in).

If you can't verify something (e.g., no Docker available in your environment), say so explicitly rather than claiming success.
