# SpongeCoach

Floorball team collaboration MVP. Backend in `backend/` (Quarkus), frontend in `frontend/` (Angular), Postgres via docker-compose. See [CONTEXT.md](CONTEXT.md) and `docs/adr/` for the domain model and architecture decisions.

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

- Backend: `./gradlew test` (acceptance tests via `@QuarkusTest` + RestAssured, run against Quarkus Dev Services / Testcontainers — no local Postgres needed).
- Frontend unit/component tests: `npm test` (Jest).
- Frontend e2e: `npm run e2e` (Playwright).
