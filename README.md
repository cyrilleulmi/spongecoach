# SpongeCoach

Floorball team collaboration MVP. Backend in `backend/` (Quarkus), frontend in `frontend/` (Angular), Postgres via docker-compose.

| Where | What |
|---|---|
| [CONTEXT.md](CONTEXT.md) | domain language — the words, and the ones to avoid |
| [docs/architecture.md](docs/architecture.md) | how it fits together: layers, data model, API, frontend rules |
| [docs/adr/](docs/adr/) | why it is built this way |
| [docs/spec/](docs/spec/) | what it does — Gherkin scenarios, each traced to the test that proves it |

## Local development

Requires Docker, JDK 25, and Node (Angular CLI needs Node ^22.22.3 || ^24.15.0 || >=26.0.0).

1. Start Postgres:

   ```
   docker-compose up -d
   ```

2. In one terminal, run the backend:

   ```
   cd backend
   ./gradlew quarkusDev
   ```

   Serves on http://localhost:8080.

3. In another terminal, run the frontend:

   ```
   cd frontend
   npm start
   ```

   Serves on http://localhost:4200.

## Testing

Run everything with one command from the repo root:

```
.\verify.ps1            # spec traceability, backend, frontend, e2e
.\verify.ps1 -SkipE2e   # without Playwright
.\verify.ps1 -Only backend
```

It prints a stage-by-stage verdict and exits non-zero if any stage fails. Docker must be running for the backend stage; it is reported as skipped, not passed, if it is not.

The stages individually:

- Spec traceability: `node scripts/check-spec-coverage.mjs` — every scenario in `docs/spec/` must name the test that proves it. See [docs/spec/README.md](docs/spec/README.md).
- Backend: `cd backend && ./gradlew test` (acceptance tests via `@QuarkusTest` + RestAssured, run against Quarkus Dev Services / Testcontainers — no local Postgres needed).
- Frontend unit/component tests: `cd frontend && npm test` (Jest).
- Frontend e2e: `cd frontend && npm run e2e` (Playwright; the API is stubbed in the browser, no backend needed).
