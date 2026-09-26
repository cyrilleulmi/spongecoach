---
name: verify
description: Run SpongeCoach (Postgres + Quarkus dev + Angular dev) and drive the real app — API via curl, UI via Playwright — to verify a change at its surface. Use when verifying a change works in the running app, not in tests.
---

# Verify SpongeCoach at runtime

## Start (Windows, Git Bash)

1. Docker must be up. If `docker info` fails, start
   `"C:\Program Files\Docker\Docker\Docker Desktop.exe"` and poll `docker info` until it answers.
2. From the repo root, in the background:
   - `docker compose up -d` — Postgres on :5432 (user/db `spongecoach`)
   - `cd backend && ./gradlew.bat quarkusDev --console=plain > <scratch>/qdev.log 2>&1` — :8080.
     Flyway migrates the **dev DB** at start; grep the log for `Migrating schema` to confirm a new migration applied.
   - `cd frontend && npm start > <scratch>/fdev.log 2>&1` — :4200, proxies `/api` to :8080
3. Ready when `curl -s -o /dev/null -w "%{http_code}" localhost:8080/api/users` is 200 and :4200 answers 200.

## Who you are (ADR-0017)

Every `/api` call except `GET /api/users` needs the `spongecoach-user=<app_user id>` cookie, or it returns 401.

- Admin (SysAdmin): `90000000-0000-0000-0000-000000000001`
- Cyrille, Jan, Samuel (Coach): `…0002`–`…0004`; Anita (Coach + Player): `…0005`
- Player users have random ids: look them up by name from `GET /api/users`
- curl: `curl -b "spongecoach-user=<id>" -H 'Content-Type: application/json' …`

## Driving the UI

Write a CommonJS script in the scratchpad and run it with node:
`const { chromium } = require('C:/Workspace/spongecoach/frontend/node_modules/playwright');`
- Switch user: `page.getByLabel('Benutzer wechseln').selectOption(<id>)`. The page reloads.
- Screenshot to the scratchpad, then Read the PNG to look at it.

## Seed data worth knowing

- Lines: Kiwi `10000000-…0001` (Carmela, Debi, Gina, Nives), Bäri `…0002`, Lama `…0003`, Goalies `…0004`
- Players: `20000000-…0001` Carmela … `…0009` Anita
- Iteration 1 "Vorbereitung" is entirely in the past, and the timeline opens on it. Click the
  iteration selector's `›` button to reach upcoming Events (Iteration 2).

## Gotchas

- Tools: Python 3.13 (`python`), `jq`, `yq`, `fd`, `rg`, `gh`, node, and curl. They were installed by
  winget, so a shell or Claude session started before the install doesn't have them on PATH. Restart it.
  There's no local `psql`; use `docker exec` (below).
- Pull ids out of JSON with jq, e.g.
  `curl -s localhost:8080/api/users | jq -r '.[] | select(.name=="Carmela") | .id'`
- Git Bash mangles non-ASCII characters (`ä`) in curl `-d` bodies, which gives a generic 400. Use ASCII
  bodies, or `--data-binary @file.json`.
- In `node -e`, use `C:/...` paths, not `/c/...`.
- Writes hit the real dev DB: note what you change and revert it (psql via
  `docker exec $(docker ps -qf name=postgres) psql -U spongecoach -d spongecoach`).
- Leave the servers running or stop them, but say which in the report.
