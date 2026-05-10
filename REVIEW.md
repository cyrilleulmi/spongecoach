# SpongeCoach — Implementation Review

This document covers everything implemented in the first two sessions. Use it to review the work before the next coding session.

---

## What was built

Full backend (Club, Person, Team) + Angular frontend skeleton, following the hexagonal architecture defined in AGENTS.md.

**Test result: 51 / 51 passing** (JDK 25, Docker, Testcontainers PostgreSQL).

---

## Backend

### Stack adjustments vs. AGENTS.md spec

The scaffold used a pre-release **Spring Boot 4.0.6** which introduced breaking changes worth knowing for future work:

| Topic | Spring Boot 3.x (original spec) | Spring Boot 4.0.6 (actual) |
|---|---|---|
| Jackson namespace | `com.fasterxml.jackson.databind` | `tools.jackson.databind` |
| `@WebMvcTest` package | `o.s.boot.test.autoconfigure.web.servlet` | `o.s.boot.webmvc.test.autoconfigure` |
| `@AutoConfigureMockMvc` | same old package | `o.s.boot.webmvc.test.autoconfigure` |
| `TestRestTemplate` | available | **removed** — use MockMvc + `@AutoConfigureMockMvc` |
| Test starters | `spring-boot-starter-test` only | `spring-boot-starter-test` + `spring-boot-starter-webmvc-test` |
| Spring version | 6.x | 7.0.7 |

Also: **ArchUnit 1.4.2** is required (1.4.0 silently fails to parse Java 25 class files — all rules report "no classes to check").

The main application class was moved from `coach.spongecoach.backend.BackendApplication` to **`coach.spongecoach.BackendApplication`** (root package). Without this, `@SpringBootTest` in feature packages like `coach.spongecoach.club.*` cannot locate the `@SpringBootApplication` by upward scanning.

### Package layout

```
backend/src/main/java/coach/spongecoach/
├── BackendApplication.java              ← root package, scans all of coach.spongecoach
├── club/
│   ├── domain/
│   │   ├── model/Club.java              ← record: id, name, location
│   │   └── ClubRepository.java          ← port interface
│   ├── application/
│   │   ├── ClubService.java
│   │   └── ClubNotFoundException.java   ← @ResponseStatus(404)
│   └── adapter/
│       ├── in/web/
│       │   ├── ClubController.java
│       │   ├── CreateClubRequest.java   ← @NotBlank validated
│       │   ├── UpdateClubRequest.java
│       │   └── ClubResponse.java
│       └── out/persistence/
│           ├── ClubJpaEntity.java
│           ├── SpringDataClubRepository.java
│           ├── ClubMapper.java
│           └── ClubPersistenceAdapter.java
├── person/                              ← same shape as club
│   └── ...                             ← adds DuplicateEmailException (@ResponseStatus 409)
│                                        ← @Email validation on request DTOs
└── team/                               ← same shape; TeamService depends on ClubRepository
    └── ...                             ← FK to club (ON DELETE CASCADE in schema)
```

### Domain models

All three are **immutable records** with factory methods and `with*` mutators:

```java
// Club.java
public record Club(UUID id, String name, String location) {
    public static Club create(String name, String location) { ... }  // generates UUID
    public Club withName(String name) { ... }
    public Club withLocation(String location) { ... }
}

// Person.java
public record Person(UUID id, String firstName, String lastName, String email) { ... }

// Team.java
public record Team(UUID id, String name, UUID clubId) { ... }
```

UUIDs are generated in the domain (`UUID.randomUUID()`), never delegated to JPA.

### Cross-feature dependency

`TeamService` (in `team.application`) depends on `ClubRepository` (in `club.domain`) to validate that the referenced club exists before creating a team. This is cross-feature collaboration through a domain port — permitted by the AGENTS.md rule ("Cross-feature collaboration goes through application services or domain ports").

### REST API surface

| Method | Path | Success | Error |
|---|---|---|---|
| POST | `/api/clubs` | 201 + body | 400 if blank fields |
| GET | `/api/clubs` | 200 + array | — |
| GET | `/api/clubs/{id}` | 200 + body | 404 |
| PUT | `/api/clubs/{id}` | 200 + body | 404, 400 |
| DELETE | `/api/clubs/{id}` | 204 | 404 |
| POST | `/api/persons` | 201 + body | 400 (validation), 409 (duplicate email) |
| GET | `/api/persons` | 200 + array | — |
| GET | `/api/persons/{id}` | 200 + body | 404 |
| PUT | `/api/persons/{id}` | 200 + body | 404, 400, 409 |
| DELETE | `/api/persons/{id}` | 204 | 404 |
| POST | `/api/teams` | 201 + body | 400, 404 (unknown club) |
| GET | `/api/teams` | 200 + array | — |
| GET | `/api/teams?clubId=` | 200 + filtered array | — |
| GET | `/api/teams/{id}` | 200 + body | 404 |
| PUT | `/api/teams/{id}` | 200 + body (name only) | 404, 400 |
| DELETE | `/api/teams/{id}` | 204 | 404 |

Error responses are plain Spring Boot error bodies (no custom envelope yet).

### Database schema (Liquibase)

`db/changelog/db.changelog-master.yaml` includes in order:

1. `001-create-club-table.yaml` — `id uuid PK, name varchar(255) NOT NULL, location varchar(255) NOT NULL`
2. `002-create-person-table.yaml` — `id uuid PK, first_name, last_name, email varchar(255) NOT NULL UNIQUE`
3. `003-create-team-table.yaml` — `id uuid PK, name, club_id uuid NOT NULL, FK → club(id) ON DELETE CASCADE`

JPA runs with `ddl-auto=validate` — Liquibase owns the schema.

### ArchUnit rules enforced

File: `backend/src/test/java/coach/spongecoach/architecture/ArchitectureTest.java`

1. `domainMustNotDependOnSpringOrJpaOrAdapters` — domain is pure Java
2. `applicationMustNotDependOnAdapters` — services don't touch JPA/web
3. `adaptersMustNotCrossFeatureBoundaries` — club/person/team adapters are isolated
4. `noFeatureCycles` — slice dependency graph is acyclic
5. `restControllerOnlyInWebAdapter` — `@RestController` only in `..adapter.in.web..`
6. `entityOnlyInPersistenceAdapter` — `@Entity` only in `..adapter.out.persistence..`
7. `springDataRepositoriesOnlyInPersistenceAdapter` — same scope
8. `domainModelMustNotCarryPersistenceOrWebAnnotations` — domain records are clean

### Test coverage

| Test class | Type | What it covers |
|---|---|---|
| `ClubTest`, `PersonTest`, `TeamTest` | Unit | Domain factory methods and `with*` mutators |
| `ClubServiceTest`, `PersonServiceTest`, `TeamServiceTest` | Unit | Use-case logic against in-memory port implementations |
| `ClubControllerTest`, `PersonControllerTest`, `TeamControllerTest` | `@WebMvcTest` slice | HTTP contract, validation, 404/400/409 error mapping |
| `ClubIntegrationTest`, `PersonIntegrationTest`, `TeamIntegrationTest` | Testcontainers | Full CRUD lifecycle against real PostgreSQL; cross-feature FK enforcement |
| `ArchitectureTest` | ArchUnit | 8 hexagonal boundary rules |
| `BackendApplicationTests` | Spring context | Context loads cleanly |

Integration tests use `@SpringBootTest` + `@AutoConfigureMockMvc` (not a running HTTP server — MockMvc dispatches directly into the servlet context backed by a real DB).

---

## Frontend

### Structure

```
frontend/src/
├── styles.scss              ← global utilities (btn, card, table, form-field, page)
├── styles/
│   └── _theme.scss          ← ALL design tokens as CSS custom properties
└── app/
    ├── app.ts               ← shell with nav bar
    ├── app.html             ← <nav> + <router-outlet>
    ├── app.scss             ← nav styles (vars only, no hard-coded values)
    ├── app.routes.ts        ← lazy-loaded routes
    ├── app.config.ts        ← provideHttpClient() wired here
    ├── core/api/
    │   ├── club.api.ts      ← ClubApiService (typed, inject-based)
    │   ├── person.api.ts    ← PersonApiService
    │   └── team.api.ts      ← TeamApiService
    └── features/
        ├── clubs/
        │   ├── clubs.component.*         ← list + inline create/edit form
        │   └── club-detail.component.*   ← club info + teams CRUD
        └── persons/
            └── persons.component.*       ← list + inline create/edit form
```

### Design token approach

All colours, spacing, font sizes, radii, and shadows live in `src/styles/_theme.scss` as CSS custom properties on `:root`. Components reference variables only (`var(--color-primary)`, etc.). Reskinning the app means editing one file.

### Routes

| Path | Component | Notes |
|---|---|---|
| `/` | redirect → `/clubs` | |
| `/clubs` | `ClubsComponent` | List + create/edit inline |
| `/clubs/:id` | `ClubDetailComponent` | Club info + teams CRUD |
| `/persons` | `PersonsComponent` | List + create/edit inline |

All routes are **lazy-loaded** (`loadComponent`).

### API proxy

`proxy.conf.json` forwards `/api/*` → `http://localhost:8080` during `ng serve`. Wired into `angular.json` under `serve.options.proxyConfig`.

### What's not yet in the frontend

- Teams list page (teams are managed from the Club detail page; a standalone `/teams` route doesn't exist yet)
- Any form of error toast / notification component
- `npm test` — the generated Karma spec for `AppComponent` was left as-is; no feature-specific frontend specs were written

---

## Infrastructure / Docker

### `docker-compose.dev.yml`

Postgres only — for the native dev loop (`.\gradlew.bat bootRun` + `ng serve`):

```
postgres:16 → localhost:5432
  DB: spongecoach / user: spongecoach / pw: spongecoach
```

### `docker-compose.yml`

Full stack:

```
postgres → backend (8080) → frontend nginx (4200)
```

Frontend nginx (`frontend/nginx.conf`) proxies `/api/` to the backend container and serves Angular as a SPA (`try_files $uri /index.html`).

Dockerfiles: `backend/Dockerfile` (multi-stage, Temurin 17 JRE final image), `frontend/Dockerfile` (node:24 build, nginx:alpine serve).

### Dev environment notes

- **JDK 25** installed at `C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot`
- **Docker Desktop** installed; CLI at `C:\Program Files\Docker\Docker\resources\bin\docker.exe` (not on PATH by default in new PowerShell sessions — add manually or set permanently)
- To run the full test suite from a new terminal:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
  $env:PATH = "C:\Program Files\Docker\Docker\resources\bin;$env:JAVA_HOME\bin;$env:PATH"
  cd backend
  .\gradlew.bat test
  ```

---

## Things to review / decide before next session

1. **Teams standalone page** — should there be a `/teams` list route, or is managing teams from the Club detail page enough?
2. **Error presentation** — currently errors surface as plain text `<p>` elements. A toast/snackbar component would be cleaner.
3. **JPA entity visibility** — all entity fields and constructors are package-private (not `public`). This is intentional to enforce that nothing outside the persistence package touches them. Worth confirming this matches your preference.
4. **Team update** — `PUT /api/teams/{id}` only accepts `name` (the club of a team cannot be changed after creation). If team re-assignment is needed, the domain and schema will need extending.
5. **`docker-compose.yml` Dockerfile base images** — the backend Dockerfile uses `eclipse-temurin:17-jre` (not 25, since 25 JRE images may not be on Docker Hub yet under that tag). Confirm this is acceptable or update the tag.
6. **Frontend tests** — no Angular component specs were written. The existing generated `app.spec.ts` is stale and will likely fail. Decide whether to write specs before or after the next feature.
7. **Commit structure** — all work is still unstaged. Per AGENTS.md, commits should go feature-by-feature (domain → application → adapters → frontend). Ready to commit whenever you say so.
